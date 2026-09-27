package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.RegisterAttendancePort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.infrastructure.controller.graphql.dto.RegisterAttendanceInput;
import com.smms.assistance.infrastructure.messaging.OutboxEventPublisher;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.infrastructure.storage.S3StorageAdapter;
import com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper;
import com.smms.assistance.infrastructure.client.ProjectOutput;
import com.smms.assistance.infrastructure.client.EffectiveScheduleOutput;
import com.smms.assistance.shared.util.TimezoneService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class RegisterAttendanceUseCase implements RegisterAttendancePort {

    @Inject AttendanceRepository attendanceRepository;
    @Inject OutboxEventPublisher outboxPublisher;
    @Inject S3StorageAdapter s3StorageAdapter;
    @Inject ProjectManagerClientWrapper pmClientWrapper;
    @Inject com.smms.assistance.infrastructure.client.AlertsClient alertsClient;
    @Inject AbsenceRepository absenceRepository;
    @Inject org.eclipse.microprofile.jwt.JsonWebToken jwt;
    @Inject org.eclipse.microprofile.context.ManagedExecutor executor;

    @Override
    @Transactional
    public AttendanceEntity execute(UUID userId, RegisterAttendanceInput input) {

        java.time.LocalDateTime timestamp = TimezoneService.utcNow();
        System.out.println("[Audit Log] Attendance registration attempt - userId: " + userId 
                + ", projectId: " + input.getProjectId() 
                + ", timestamp: " + timestamp);

        List<ProjectOutput> assignedProjects;
        try {
            assignedProjects = pmClientWrapper.getMyProjects();
        } catch (Exception e) {
            System.out.println("[Audit Log] Attendance registration failed - userId: " + userId 
                    + ", projectId: " + input.getProjectId() 
                    + ", timestamp: " + timestamp 
                    + ", reason: Failed to retrieve assigned projects from Project Manager");
            throw new RuntimeException("Could not retrieve assigned projects: " + e.getMessage(), e);
        }

        boolean isAssigned = false;
        ProjectOutput matchedProject = null;
        if (assignedProjects != null && input.getProjectId() != null) {
            for (ProjectOutput project : assignedProjects) {
                if (project.id != null && project.id.equals(input.getProjectId())) {
                    isAssigned = true;
                    matchedProject = project;
                    break;
                }
            }
        }

        boolean isGlobalAdminOrPM = false;
        if (jwt != null && jwt.getGroups() != null) {
            java.util.Set<String> groups = jwt.getGroups();
            if (groups.contains("ADMIN") || groups.contains("PROJECT_MANAGER")) {
                isGlobalAdminOrPM = true;
            }
        }

        if (input.getProjectId() == null) {
            if (!isGlobalAdminOrPM) {
                System.out.println("[Audit Log] Validation failure: projectId is null but user is not Admin/PM");
                throw new IllegalArgumentException("Debes seleccionar un proyecto.");
            }
        } else if (!isAssigned) {
            System.out.println("[Audit Log] Validation failure: projectId " + input.getProjectId() 
                    + " is not part of the user's assigned projects. userId: " + userId);
            System.out.println("[Audit Log] Attendance registration failed - userId: " + userId 
                    + ", projectId: " + input.getProjectId() 
                    + ", timestamp: " + timestamp 
                    + ", reason: Project ID is not assigned to user");
            throw new IllegalArgumentException("El proyecto no est\u00e1 asignado a este usuario.");
        }

        // SINGLE TIME SOURCE -- Project's configured timezone
        String projectTimezone = (matchedProject != null && matchedProject.getTimezone() != null)
                ? matchedProject.getTimezone()
                : TimezoneService.defaultTimezone();
        ZonedDateTime nowInProjectZone = TimezoneService.zonedNow(projectTimezone);
        LocalDate today = nowInProjectZone.toLocalDate();
        LocalTime nowTime = nowInProjectZone.toLocalTime();

        // Step 1: Check if attendance already exists for today
        boolean existsToday;
        if (input.getProjectId() == null) {
            existsToday = attendanceRepository
                    .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                    .firstResultOptional()
                    .isPresent();
        } else {
            existsToday = attendanceRepository
                    .find("userId = ?1 and projectId = ?2 and date = ?3", userId, input.getProjectId(), today)
                    .firstResultOptional()
                    .isPresent();
        }

        System.out.println("[Attendance] EXISTS_TODAY=" + existsToday
                + " | userId=" + userId + " projectId=" + input.getProjectId() + " date=" + today);

        // Step 2: If exists, evaluate replace flag BEFORE any error
        if (existsToday) {
            boolean shouldReplace = Boolean.TRUE.equals(input.getReplace());
            System.out.println("[Attendance] REPLACE_ENABLED=" + shouldReplace);

            if (shouldReplace) {
                long deleted;
                if (input.getProjectId() == null) {
                    deleted = attendanceRepository.delete(
                            "userId = ?1 and projectId is null and date = ?2",
                            userId, today
                    );
                } else {
                    deleted = attendanceRepository.delete(
                            "userId = ?1 and projectId = ?2 and date = ?3",
                            userId, input.getProjectId(), today
                    );
                }
                System.out.println("[Attendance] DELETED_AND_RECREATED | deleted=" + deleted
                        + " records for userId=" + userId);
            } else {
                throw new IllegalStateException("Ya registraste tu asistencia para este proyecto hoy.");
            }
        }

        // Classify attendance based on effective schedule from PM
        EffectiveScheduleOutput schedule = null;
        try {
            schedule = pmClientWrapper.getEffectiveSchedule(userId, input.getProjectId(), today);
        } catch (Exception e) {
            System.out.println("[Attendance] Failed to retrieve effective schedule, fallback to project config: " + e.getMessage());
        }

        String attendanceStatus = classifyAttendance(schedule, matchedProject, nowTime);
        System.out.println("[Attendance] CLASSIFICATION=" + attendanceStatus
                + " | checkIn=" + nowTime
                + " | workStart=" + (schedule != null ? schedule.expectedStartTime : "null")
                + " | grace=" + (schedule != null ? schedule.graceMinutes : 0));

        UUID attendanceId = UUID.randomUUID();

        String photoUrl = null;
        String alertsPhotoUrl = null;

        if (input.getPhotoUrl() != null && !input.getPhotoUrl().isBlank()) {

            try {

                String base64 = input.getPhotoUrl();

                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }

                byte[] decoded = Base64.getDecoder().decode(base64);

                if (decoded.length == 0) {
                    throw new RuntimeException("Empty image payload");
                }

                ByteArrayInputStream stream = new ByteArrayInputStream(decoded);

                String folder = "document/private/user/" + userId + "/photo";
                String filename = attendanceId + ".jpg";

                String key = s3StorageAdapter.upload(
                        folder,
                        filename,
                        stream,
                        decoded.length,
                        "image/jpeg"
                );

                photoUrl = key;
                alertsPhotoUrl = s3StorageAdapter.generateInternalPresignedUrl(key, 120);

            } catch (Exception e) {

                throw new RuntimeException(
                        "Error uploading photo to MinIO: " + e.getMessage(),
                        e
                );
            }
        }

        AttendanceEntity entity = AttendanceEntity.builder()
                .id(attendanceId)
                .userId(userId)
                .projectId(input.getProjectId())
                .date(today)
                .checkIn(nowTime)
                .checkOut(null)
                .status(attendanceStatus)
                .latitude(input.getLatitude())
                .longitude(input.getLongitude())
                .photoUrl(photoUrl)
                .projectTimezone(matchedProject != null && matchedProject.getTimezone() != null
                        ? matchedProject.getTimezone() : TimezoneService.defaultTimezone())
                .build();

        attendanceRepository.persist(entity);

        // Absence generation for problematic check-ins
        // OUTSIDE_SCHEDULE: user checked in outside their shift window → no absence (attendance already recorded)
        // LATE/EARLY: user showed up at wrong time → generate absence with specific type
        if ("EARLY".equals(attendanceStatus) || "LATE".equals(attendanceStatus)) {
            boolean hasAbsence;
            if (input.getProjectId() == null) {
                hasAbsence = absenceRepository
                        .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                        .firstResultOptional()
                        .isPresent();
            } else {
                hasAbsence = absenceRepository
                        .find("userId = ?1 and projectId = ?2 and date = ?3", userId, input.getProjectId(), today)
                        .firstResultOptional()
                        .isPresent();
            }
            if (!hasAbsence) {
                boolean onVacation = false;
                boolean isHoliday = false;
                try {
                    onVacation = pmClientWrapper.hasApprovedVacation(userId, today);
                } catch (Exception e) {
                    System.out.println("[Attendance] Failed to check vacation status, proceeding: " + e.getMessage());
                }
                try {
                    isHoliday = pmClientWrapper.isHoliday(input.getProjectId(), today);
                } catch (Exception e) {
                    System.out.println("[Attendance] Failed to check holiday, proceeding: " + e.getMessage());
                }
                if (onVacation) {
                    System.out.println("[Attendance] SKIPPED absence — user " + userId + " has approved vacation on " + today);
                } else if (isHoliday) {
                    System.out.println("[Attendance] SKIPPED absence — today is holiday for project " + input.getProjectId());
                } else {
                    String absenceType = "LATE".equals(attendanceStatus) ? "TARDANZA" : "TEMPRANO";
                    AbsenceEntity absence = AbsenceEntity.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .projectId(input.getProjectId())
                            .date(today)
                            .type(absenceType)
                            .justified(false)
                            .build();
                    absenceRepository.persist(absence);
                    System.out.println("[Attendance] " + absenceType + " auto-generated for " + attendanceStatus + " check-in — userId="
                            + userId + ", projectId=" + input.getProjectId() + ", date=" + today);
                }
            }
        }

        String outboxPhotoUrl = alertsPhotoUrl != null ? alertsPhotoUrl : photoUrl;
        String userEmail = jwt != null ? jwt.getClaim("email") : null;
        String userName = jwt != null ? jwt.getClaim("name") : null;
        if (userEmail == null || userEmail.isBlank()) {
            userEmail = "";
        }
        if (userName == null || userName.isBlank()) {
            userName = userEmail.contains("@") ? userEmail.split("@")[0] : "Colaborador";
        }
        userEmail = userEmail.replace("\"", "\\\"");
        userName = userName.replace("\"", "\\\"");

        outboxPublisher.publish(
                "Attendance",
                entity.getId(),
                "attendance_registered",
                String.format(
                        "{\"attendanceId\":\"%s\",\"userId\":\"%s\",\"photoUrl\":\"%s\",\"userEmail\":\"%s\",\"userName\":\"%s\"}",
                        entity.getId(),
                        userId,
                        outboxPhotoUrl != null ? outboxPhotoUrl : "",
                        userEmail,
                        userName
                )
        );

        System.out.println("✅ [Audit Log] Attendance registration successful - userId: " + userId 
                + ", projectId: " + input.getProjectId() 
                + ", status: " + attendanceStatus
                + ", checkIn: " + entity.getCheckIn()
                + ", timestamp: " + timestamp 
                + ", attendanceId: " + entity.getId());

        // --- ASYNC ALERT ORCHESTRATION ---
        final UUID finalUserId = userId;
        final UUID finalProjectId = entity.getProjectId();
        final UUID finalAttendanceId = entity.getId();
        final Double finalLat = entity.getLatitude();
        final Double finalLng = entity.getLongitude();
        final String finalPhotoUrl = entity.getPhotoUrl();
        final String finalAlertsPhotoUrl = alertsPhotoUrl;
        final ProjectOutput finalMatchedProject = matchedProject;
        final String finalDateStr = today.toString();
        final String finalCheckInStr = nowTime.toString();
        final boolean isPhotoEmpty = input.getPhotoUrl() == null || input.getPhotoUrl().isBlank();
        final String rawToken = jwt != null ? jwt.getRawToken() : null;

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // --- ORCHESTRATE NO-PHOTO ALERT ---
                if (isPhotoEmpty) {
                    System.out.println("📸 [Orchestrator] No photo provided. Creating NO_PHOTO alert asynchronously.");
                    pmClientWrapper.createAlert(
                            finalUserId,
                            finalProjectId,
                            "NO_PHOTO",
                            "Asistencia sin foto",
                            finalLat,
                            finalLng,
                            rawToken
                    );
                }

                // --- ORCHESTRATE STATELESS ALERTS ANALYSIS ---
                if (finalMatchedProject != null) {
                    System.out.println("📝 [Orchestrator] Match result: Project details retrieved successfully for projectId: " + finalProjectId);
                    
                    var alertPayload = new com.smms.assistance.infrastructure.client.AlertRequestPayload(
                        finalAttendanceId,
                        finalUserId,
                        finalProjectId,
                        finalDateStr,
                        finalCheckInStr,
                        finalLat,
                        finalLng,
                        finalAlertsPhotoUrl != null ? finalAlertsPhotoUrl : finalPhotoUrl,
                        finalMatchedProject.getLatitude(),
                        finalMatchedProject.getLongitude(),
                        finalMatchedProject.getRadius(),
                        finalMatchedProject.getWorkStartTime(),
                        finalMatchedProject.getWorkEndTime(),
                        finalMatchedProject.getGraceMinutes()
                    );

                    System.out.println("📝 [Orchestrator] Payload generated for Alerts Service. Sending asynchronously...");
                    
                    com.smms.assistance.infrastructure.client.AlertResponsePayload aiResult = alertsClient.sendAlert(alertPayload);
                    
                    if (aiResult != null) {
                        System.out.println("📝 [Orchestrator] Response received from Alerts Service: alert=" + aiResult.alert);
                        if (aiResult.alert) {
                            System.out.println("🚨 [Orchestrator] Decision: alert created. Calling Project Manager CreateAlert mutation...");
                            pmClientWrapper.createAlert(
                                finalUserId,
                                finalProjectId,
                                aiResult.type != null ? aiResult.type : "UNKNOWN",
                                aiResult.message != null ? aiResult.message : "AI validation flagged anomaly",
                                finalLat,
                                finalLng,
                                rawToken
                            );
                        } else {
                            System.out.println("✅ [Orchestrator] Decision: alert ignored (no anomaly detected).");
                        }
                    }
                } else {
                    System.out.println("⚠️ [Orchestrator] Project details not found during matching. Skipping alert analysis.");
                }
            } catch (Exception e) {
                System.out.println("❌ [Orchestrator] Error from AI service or alert persistence: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return entity;
    }

    private String classifyAttendance(EffectiveScheduleOutput schedule, ProjectOutput project, LocalTime checkIn) {
        if (schedule != null) {
            if (!schedule.workDay) {
                System.out.println("[Classification] Not a work day according to effective schedule. OUTSIDE_SCHEDULE.");
                return "OUTSIDE_SCHEDULE";
            }
            if (schedule.expectedStartTime == null) {
                System.out.println("[Classification] No schedule defined. Defaulting to CHECKED_IN.");
                return "CHECKED_IN";
            }
            try {
                LocalTime workStart = LocalTime.parse(schedule.expectedStartTime);
                int grace = schedule.graceMinutes != null ? schedule.graceMinutes : 0;
                LocalTime deadline = workStart.plusMinutes(grace);

                LocalTime workEnd = null;
                if (schedule.expectedEndTime != null && !schedule.expectedEndTime.isBlank()) {
                    workEnd = LocalTime.parse(schedule.expectedEndTime);
                }

                return determineStatus(checkIn, workStart, workEnd, deadline, grace);
            } catch (Exception e) {
                System.out.println("[Classification] Error parsing schedule times: " + e.getMessage() + ". Defaulting to CHECKED_IN.");
                return "CHECKED_IN";
            }
        }

        // Fallback
        if (project == null || project.getWorkStartTime() == null) {
            System.out.println("[Classification] No schedule defined. Defaulting to CHECKED_IN.");
            return "CHECKED_IN";
        }

        try {
            LocalTime workStart = LocalTime.parse(project.getWorkStartTime());
            int grace = project.getGraceMinutes() != null ? project.getGraceMinutes() : 0;
            LocalTime deadline = workStart.plusMinutes(grace);

            LocalTime workEnd = null;
            if (project.getWorkEndTime() != null && !project.getWorkEndTime().isBlank()) {
                workEnd = LocalTime.parse(project.getWorkEndTime());
            }

            return determineStatus(checkIn, workStart, workEnd, deadline, grace);
        } catch (Exception e) {
            System.out.println("[Classification] Error parsing schedule times: " + e.getMessage()
                    + ". Defaulting to CHECKED_IN.");
            return "CHECKED_IN";
        }
    }

    private String determineStatus(LocalTime checkIn, LocalTime workStart, LocalTime workEnd, LocalTime deadline, int grace) {
        // Night shift detection (workEnd < workStart, e.g. 22:00-06:00)
        if (workEnd != null && workEnd.isBefore(workStart)) {
            // Night shift: valid check-in window is [workStart, 23:59] or [00:00, workEnd]
            boolean isInAfterMidnight = !checkIn.isAfter(workEnd) && !checkIn.isBefore(LocalTime.MIDNIGHT);
            boolean isInSameEvening = !checkIn.isBefore(workStart);

            if (!isInSameEvening && !isInAfterMidnight) {
                System.out.println("[Classification] OUTSIDE_SCHEDULE (night shift) — checkIn=" + checkIn
                        + ", workStart=" + workStart + ", workEnd=" + workEnd
                        + ", graceMinutes=" + grace);
                return "OUTSIDE_SCHEDULE";
            }

            // EARLY only applies to the same-evening window (2h before workStart)
            if (isInSameEvening && checkIn.isBefore(workStart.minusHours(2))) {
                System.out.println("[Classification] EARLY (night shift) — checkIn=" + checkIn
                        + ", workStart=" + workStart + ", workEnd=" + workEnd
                        + ", graceMinutes=" + grace);
                return "EARLY";
            }

            if (isInSameEvening) {
                // Arrived in [workStart, 23:59] — normal on-time/late check
                if (checkIn.equals(deadline) || checkIn.isBefore(deadline)) {
                    System.out.println("[Classification] ON_TIME (night shift) — checkIn=" + checkIn
                            + ", workStart=" + workStart + ", deadline=" + deadline
                            + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
                    return "ON_TIME";
                } else {
                    System.out.println("[Classification] LATE (night shift) — checkIn=" + checkIn
                            + ", workStart=" + workStart + ", deadline=" + deadline
                            + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
                    return "LATE";
                }
            }

            // Arrived after midnight [00:00, workEnd] — always late vs 22:00 start
            System.out.println("[Classification] LATE (night shift, post-midnight) — checkIn=" + checkIn
                    + ", workStart=" + workStart + ", deadline=" + deadline
                    + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
            return "LATE";
        }

        // Day shift (standard): workStart < workEnd
        LocalTime earliestAllowed = workStart.minusHours(2);
        if (checkIn.isBefore(earliestAllowed)) {
            System.out.println("[Classification] EARLY — checkIn=" + checkIn
                    + ", workStart=" + workStart + ", earliestAllowed=" + earliestAllowed
                    + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
            return "EARLY";
        }

        if (workEnd != null && checkIn.isAfter(workEnd)) {
            System.out.println("[Classification] OUTSIDE_SCHEDULE — checkIn=" + checkIn
                    + ", workStart=" + workStart + ", workEnd=" + workEnd
                    + ", graceMinutes=" + grace);
            return "OUTSIDE_SCHEDULE";
        }

        if (checkIn.equals(deadline) || checkIn.isBefore(deadline)) {
            System.out.println("[Classification] ON_TIME — checkIn=" + checkIn
                    + ", workStart=" + workStart + ", deadline=" + deadline
                    + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
            return "ON_TIME";
        } else {
            System.out.println("[Classification] LATE — checkIn=" + checkIn
                    + ", workStart=" + workStart + ", deadline=" + deadline
                    + ", workEnd=" + workEnd + ", graceMinutes=" + grace);
            return "LATE";
        }
    }
}