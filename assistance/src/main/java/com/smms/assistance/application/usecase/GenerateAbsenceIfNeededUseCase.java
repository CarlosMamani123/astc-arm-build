package com.smms.assistance.application.usecase;

import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.shared.util.TimezoneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@ApplicationScoped
public class GenerateAbsenceIfNeededUseCase {

    @ConfigProperty(name = "app.attendance.absence-cutoff-hour", defaultValue = "9")
    int cutoffHour;

    @ConfigProperty(name = "app.attendance.absence-cutoff-minute", defaultValue = "30")
    int cutoffMinute;

    @ConfigProperty(name = "app.attendance.default-timezone", defaultValue = "UTC")
    String defaultTimezone;

    @Inject
    AbsenceRepository absenceRepository;

    @Inject
    AttendanceRepository attendanceRepository;

    @Inject
    com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper projectManagerClientWrapper;

    @Inject
    com.smms.assistance.infrastructure.messaging.OutboxEventPublisher outboxPublisher;

    @Transactional
    public void execute(UUID userId) {
        execute(userId, null);
    }

    @Transactional
    public void execute(UUID userId, UUID projectId) {
        // Evitar generar faltas a usuarios que no tienen proyectos asignados en el sistema
        try {
            java.util.List<com.smms.assistance.infrastructure.client.ProjectOutput> myProjects = 
                    projectManagerClientWrapper.getMyProjects();
            if (myProjects == null || myProjects.isEmpty()) {
                System.out.println("[AbsenceGen] User has no projects assigned. Skipping absence generation. userId=" + userId);
                return;
            }
        } catch (Exception e) {
            System.out.println("[AbsenceGen] Failed to fetch projects for user, skipping absence generation: " + e.getMessage());
            return;
        }

        String tz = TimezoneService.isValidTimezone(defaultTimezone) ? defaultTimezone : TimezoneService.defaultTimezone();
        ZonedDateTime nowInZone = TimezoneService.zonedNow(tz);
        LocalDate today = nowInZone.toLocalDate();
        LocalTime now = nowInZone.toLocalTime();
        LocalTime cutoff = LocalTime.of(cutoffHour, cutoffMinute);

        if (now.isBefore(cutoff)) {
            System.out.println("[AbsenceGen] Before cutoff (" + cutoff + "). Skipping absence check for userId=" + userId);
            return;
        }

        boolean hasAttendance = attendanceRepository
                .find("userId = ?1 and date = ?2 and status not in (?3, ?4)",
                        userId, today, "OUTSIDE_SCHEDULE", "EARLY")
                .firstResultOptional()
                .isPresent();

        boolean hasAbsence;
        if (projectId != null) {
            hasAbsence = absenceRepository
                    .find("userId = ?1 and projectId = ?2 and date = ?3", userId, projectId, today)
                    .firstResultOptional()
                    .isPresent();
        } else {
            hasAbsence = absenceRepository
                    .find("userId = ?1 and date = ?2", userId, today)
                    .firstResultOptional()
                    .isPresent();
        }

        if (hasAttendance || hasAbsence) {
            System.out.println("[AbsenceGen] User already has attendance or absence for today. Skipping. userId=" + userId);
            return;
        }

        // Check if today is a holiday for any of the user's projects
        boolean isHolidayToday = false;
        try {
            java.util.List<com.smms.assistance.infrastructure.client.ProjectOutput> projects = projectManagerClientWrapper.getMyProjects();
            if (projects != null) {
                for (com.smms.assistance.infrastructure.client.ProjectOutput p : projects) {
                    if (p.id != null && projectManagerClientWrapper.isHoliday(p.id, today)) {
                        isHolidayToday = true;
                        System.out.println("[AbsenceGen] Today " + today + " is a holiday for project " + p.name + ". Skipping absence.");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AbsenceGen] Failed to check holidays: " + e.getMessage());
        }

        if (isHolidayToday) {
            return;
        }

        // Check if user has approved vacation for today
        boolean onVacation = false;
        try {
            onVacation = projectManagerClientWrapper.hasApprovedVacation(userId, today);
            if (onVacation) {
                System.out.println("[AbsenceGen] User " + userId + " has approved vacation on " + today + ". Skipping absence.");
            }
        } catch (Exception e) {
            System.out.println("[AbsenceGen] Failed to check vacation: " + e.getMessage());
        }

        if (onVacation) {
            return;
        }

        AbsenceEntity absence = AbsenceEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .projectId(projectId)
                .date(today)
                .type("FALTA_AUTOMATICA")
                .justified(false)
                .build();

        absenceRepository.persist(absence);

        System.out.println("[AbsenceGen] ABSENCE generated for userId=" + userId
                + ", projectId=" + projectId + ", date=" + today + ", cutoff=" + cutoff + ". No attendance found.");

        try {
            outboxPublisher.publish(
                    "Absence",
                    absence.getId(),
                    "absence_no_record",
                    String.format(
                            "{\"absenceId\":\"%s\",\"userId\":\"%s\",\"userEmail\":\"\",\"userName\":\"\"}",
                            absence.getId(),
                            userId
                    )
            );
        } catch (Exception e) {
            System.out.println("[AbsenceGen] Error publishing outbox event: " + e.getMessage());
        }
    }
}