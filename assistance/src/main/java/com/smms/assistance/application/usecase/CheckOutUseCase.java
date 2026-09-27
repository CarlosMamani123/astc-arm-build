package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.CheckOutPort;
import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper;
import com.smms.assistance.infrastructure.client.ProjectOutput;
import com.smms.assistance.infrastructure.client.EffectiveScheduleOutput;
import com.smms.assistance.shared.util.TimezoneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CheckOutUseCase implements CheckOutPort {

    @Inject
    AttendanceRepository attendanceRepository;

    @Inject
    com.smms.assistance.infrastructure.messaging.OutboxEventPublisher outboxPublisher;

    @Inject
    org.eclipse.microprofile.jwt.JsonWebToken jwt;

    @Inject
    com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper pmClientWrapper;

    @Inject
    com.smms.assistance.infrastructure.storage.S3StorageAdapter s3StorageAdapter;

    @Override
    @Transactional
    public AttendanceEntity execute(UUID userId, UUID projectId) {
        return execute(userId, projectId, null, null, null);
    }

    @Override
    @Transactional
    public AttendanceEntity execute(UUID userId, UUID projectId, String photoUrl, Double latitude, Double longitude) {
        String projectTimezone = resolveProjectTimezone(userId, projectId);
        ZonedDateTime nowInProjectZone = TimezoneService.zonedNow(projectTimezone);
        LocalDate today = nowInProjectZone.toLocalDate();
        LocalTime nowTime = nowInProjectZone.toLocalTime();

        Optional<AttendanceEntity> existing;
        if (projectId == null) {
            existing = attendanceRepository
                    .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                    .firstResultOptional();
            if (existing.isEmpty()) {
                existing = attendanceRepository
                        .find("userId = ?1 and date = ?2 and checkOut is null order by checkIn desc", userId, today)
                        .firstResultOptional();
            }
        } else {
            existing = attendanceRepository
                    .find("userId = ?1 and projectId = ?2 and date = ?3", userId, projectId, today)
                    .firstResultOptional();
            if (existing.isEmpty()) {
                existing = attendanceRepository
                        .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                        .firstResultOptional();
            }
            if (existing.isEmpty()) {
                existing = attendanceRepository
                        .find("userId = ?1 and date = ?2 and checkOut is null order by checkIn desc", userId, today)
                        .firstResultOptional();
            }
        }

        if (existing.isEmpty()) {
            // Check if user already completed checkout today
            boolean alreadyCheckedOut = attendanceRepository
                    .find("userId = ?1 and date = ?2 and checkOut is not null", userId, today)
                    .firstResultOptional()
                    .isPresent();
            if (alreadyCheckedOut) {
                throw new IllegalStateException("Ya registraste tu salida hoy.");
            }
            throw new IllegalStateException("No hay registro de entrada para hoy. Registre su entrada primero.");
        }

        AttendanceEntity entity = existing.get();

        if (entity.getCheckOut() != null) {
            throw new IllegalStateException("Ya registraste tu salida hoy.");
        }

        entity.setCheckOut(nowTime);

        // Update location if provided
        if (latitude != null) entity.setLatitude(latitude);
        if (longitude != null) entity.setLongitude(longitude);

        // Upload and update photo if provided
        if (photoUrl != null && !photoUrl.isBlank()) {
            try {
                String base64 = photoUrl;
                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }
                byte[] decoded = java.util.Base64.getDecoder().decode(base64);
                if (decoded.length > 0) {
                    java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(decoded);
                    String folder = "document/private/user/" + userId + "/photo";
                    String filename = entity.getId() + "_checkout.jpg";
                    String key = s3StorageAdapter.upload(folder, filename, stream, decoded.length, "image/jpeg");
                    entity.setPhotoUrl(key);
                }
            } catch (Exception e) {
                System.out.println("[CheckOut] Error uploading checkout photo: " + e.getMessage());
            }
        }

        String departureStatus = classifyDeparture(userId, projectId, today, entity.getStatus(), nowTime);
        entity.setStatus(departureStatus);

        attendanceRepository.persist(entity);

        System.out.println("[Attendance] CHECK-OUT -> userId=" + userId
                + ", projectId=" + projectId
                + ", entry=" + entity.getCheckIn()
                + ", exit=" + entity.getCheckOut()
                + ", status=" + entity.getStatus());

        try {
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

            String eventType = "EARLY_DEPARTURE".equals(departureStatus) ? "checkout_early" : "checkout_registered";

            outboxPublisher.publish(
                    "Attendance",
                    entity.getId(),
                    eventType,
                    String.format(
                            "{\"attendanceId\":\"%s\",\"userId\":\"%s\",\"userEmail\":\"%s\",\"userName\":\"%s\"}",
                            entity.getId(),
                            userId,
                            userEmail,
                            userName
                    )
            );
        } catch (Exception e) {
            System.out.println("[CheckOut] Error publishing outbox event: " + e.getMessage());
        }

        return entity;
    }

    /**
     * Classify check-out against effective schedule (considers overrides and shift templates).
     * Uses getEffectiveSchedule instead of raw project workEndTime.
     */
    private String classifyDeparture(UUID userId, UUID projectId, LocalDate today, String currentStatus, LocalTime checkOutTime) {
        try {
            EffectiveScheduleOutput schedule = pmClientWrapper.getEffectiveSchedule(userId, projectId, today);

            if (schedule == null || !schedule.workDay || schedule.expectedEndTime == null || schedule.expectedEndTime.isBlank()) {
                System.out.println("[CheckOut] No work end time in effective schedule. Status unchanged: " + currentStatus);
                return currentStatus + "_CHECKED_OUT";
            }

            LocalTime workEnd = LocalTime.parse(schedule.expectedEndTime);
            int grace = schedule.graceMinutes != null ? schedule.graceMinutes : 0;

            LocalTime workStart = schedule.expectedStartTime != null && !schedule.expectedStartTime.isBlank()
                    ? LocalTime.parse(schedule.expectedStartTime) : null;

            // Night shift detection (workEnd < workStart, e.g. 22:00-06:00)
            if (workStart != null && workEnd.isBefore(workStart)) {
                boolean isPostMidnight = !checkOutTime.isAfter(workEnd) && !checkOutTime.isBefore(LocalTime.MIDNIGHT);
                boolean isSameEvening = !checkOutTime.isBefore(workStart);

                if (isPostMidnight) {
                    // Checkout after midnight but within shift end — on-time
                    System.out.println("[CheckOut] On-time departure (night shift, post-midnight) — checkOut=" + checkOutTime
                            + ", workEnd=" + workEnd);
                    return currentStatus + "_CHECKED_OUT";
                }

                if (isSameEvening) {
                    // Checkout in [workStart, 23:59] — still on shift, check if too early
                    long minutesUntilShiftEnd = java.time.Duration.between(checkOutTime, LocalTime.MAX).toMinutes()
                            + java.time.Duration.between(LocalTime.MIN, workEnd).toMinutes() + 1;
                    if (minutesUntilShiftEnd > grace + 60) {
                        System.out.println("[CheckOut] EARLY_DEPARTURE (night shift) — checkOut=" + checkOutTime
                                + ", workEnd=" + workEnd + ", minutesToEnd=" + minutesUntilShiftEnd);
                        return "EARLY_DEPARTURE";
                    }
                    return currentStatus + "_CHECKED_OUT";
                }

                // Checkout outside both windows — early
                System.out.println("[CheckOut] EARLY_DEPARTURE (outside night shift window) — checkOut=" + checkOutTime
                        + ", workStart=" + workStart + ", workEnd=" + workEnd);
                return "EARLY_DEPARTURE";
            }

            // Day shift (standard): workStart < workEnd
            if (checkOutTime.isBefore(workEnd)) {
                System.out.println("[CheckOut] EARLY_DEPARTURE — checkOut=" + checkOutTime
                        + ", workEnd=" + workEnd);
                return "EARLY_DEPARTURE";
            }

            System.out.println("[CheckOut] On-time departure — checkOut=" + checkOutTime
                    + ", workEnd=" + workEnd);
            return currentStatus + "_CHECKED_OUT";

        } catch (Exception e) {
            System.out.println("[CheckOut] Error classifying departure: " + e.getMessage());
            return currentStatus + "_CHECKED_OUT";
        }
    }

    private String resolveProjectTimezone(UUID userId, UUID projectId) {
        try {
            java.util.List<ProjectOutput> projects = pmClientWrapper.getMyProjects();
            if (projects != null) {
                for (ProjectOutput p : projects) {
                    if (p.id != null && p.id.equals(projectId) && p.getTimezone() != null
                            && TimezoneService.isValidTimezone(p.getTimezone())) {
                        return p.getTimezone();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[CheckOut] Could not resolve project timezone, using default");
        }
        return TimezoneService.defaultTimezone();
    }
}
