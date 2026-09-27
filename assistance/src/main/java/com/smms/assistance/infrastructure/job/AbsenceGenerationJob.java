package com.smms.assistance.infrastructure.job;

import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.shared.util.TimezoneService;
import com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@ApplicationScoped
public class AbsenceGenerationJob {

    private static final Logger LOG = Logger.getLogger(AbsenceGenerationJob.class);

    @Inject
    AttendanceRepository attendanceRepository;

    @Inject
    AbsenceRepository absenceRepository;

    @ConfigProperty(name = "app.absence.job.cutoff-hour", defaultValue = "9")
    int cutoffHour;

    @ConfigProperty(name = "app.absence.job.cutoff-minute", defaultValue = "30")
    int cutoffMinute;

    @Inject
    ProjectManagerClientWrapper pmClientWrapper;

    @ConfigProperty(name = "app.absence.job.default-timezone", defaultValue = "America/Lima")
    String defaultTimezone;

    // Desactivado: las faltas se generan desde el frontend al abrir el dashboard
    // y al registrar asistencia (RegisterAttendanceUseCase/GenerateAbsenceIfNeededUseCase)
    // El cutoff se configura por proyecto en /pm/creacion-horarios (absenceCutoffTime)
    // @Scheduled(cron = "{app.absence.job.cron}")
    @Transactional
    void generateAbsences(ScheduledExecution execution) {
        LOG.info("========================================");
        LOG.infof("[AbsenceJob] STARTED — %s", execution.getScheduledFireTime());
        LOG.infof("[AbsenceJob] Default cutoff: %02d:%02d, Default timezone: %s", cutoffHour, cutoffMinute, defaultTimezone);
        LOG.info("========================================");

        int absencesCreated = 0;
        int usersChecked = 0;

        // Get unique (userId, projectId) pairs from recent attendances
        Map<String, String> userTzMap = new LinkedHashMap<>(); // key "userId_projectId" -> timezone
        List<AttendanceEntity> recentAttendances = attendanceRepository
                .find("date >= ?1 ORDER BY date DESC", LocalDate.now().minusDays(90))
                .list();

        LOG.infof("[AbsenceJob] Recent attendance records: %d", recentAttendances.size());

        // Build a map of unique user+project pairs with their latest timezone
        for (AttendanceEntity att : recentAttendances) {
            if (att.getUserId() == null) continue;
            String key = att.getUserId() + "_" + (att.getProjectId() != null ? att.getProjectId() : "noproject");
            if (userTzMap.containsKey(key)) continue;
            String tz = att.getProjectTimezone();
            if (tz == null || !TimezoneService.isValidTimezone(tz)) {
                tz = defaultTimezone;
            }
            userTzMap.put(key, tz);
        }

        LOG.infof("[AbsenceJob] Unique user+project pairs: %d", userTzMap.size());

        for (Map.Entry<String, String> entry : userTzMap.entrySet()) {
            String[] parts = entry.getKey().split("_", 2);
            UUID userId = UUID.fromString(parts[0]);
            UUID projectId = "noproject".equals(parts[1]) ? null : UUID.fromString(parts[1]);
            String projectTz = entry.getValue();

            String tz = TimezoneService.isValidTimezone(projectTz) ? projectTz : defaultTimezone;
            LocalDate today = TimezoneService.todayAtZone(tz);
            LocalTime now = TimezoneService.nowAtZone(tz);
            LocalTime cutoff = LocalTime.of(cutoffHour, cutoffMinute);

            usersChecked++;

            if (now.isBefore(cutoff)) {
                LOG.debugf("[AbsenceJob] User %s: now=%s before cutoff=%s (tz=%s), skipping", userId, now, cutoff, tz);
                continue;
            }

            // Check if attendance already exists today (in project timezone)
            long attendanceToday = attendanceRepository.count(
                    "userId = ?1 and date = ?2 and status not in (?3, ?4)",
                    userId, today, "OUTSIDE_SCHEDULE", "EARLY");

            if (attendanceToday > 0) {
                continue;
            }

            // Check if absence already exists for today
            long existingAbsence = absenceRepository.countByUser(
                    userId, projectId, today, today, null, null);

            if (existingAbsence > 0) {
                continue;
            }

            // Check if today is a holiday for this project
            if (projectId != null) {
                try {
                    if (pmClientWrapper.isHoliday(projectId, today)) {
                        LOG.infof("[AbsenceJob] Today %s is a holiday for project %s, skipping user %s", today, projectId, userId);
                        continue;
                    }
                } catch (Exception e) {
                    LOG.warnf("[AbsenceJob] Failed to check holiday for project %s: %s", projectId, e.getMessage());
                }
            }

            // Check if user has approved vacation for today
            try {
                boolean onVacation = pmClientWrapper.hasApprovedVacation(userId, today);
                if (onVacation) {
                    LOG.infof("[AbsenceJob] User %s has approved vacation on %s, skipping absence", userId, today);
                    continue;
                }
            } catch (Exception e) {
                LOG.warnf("[AbsenceJob] Failed to check vacation for user %s: %s", userId, e.getMessage());
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
            absencesCreated++;

            LOG.infof("[AbsenceJob] ABSENCE CREATED — user=%s, project=%s, date=%s, tz=%s",
                    userId, projectId, today, tz);
        }

        LOG.info("========================================");
        LOG.infof("[AbsenceJob] COMPLETED — users=%d, absences=%d", usersChecked, absencesCreated);
        if (absencesCreated == 0 && usersChecked > 0) {
            LOG.info("[AbsenceJob] All users already have attendance or absence for today.");
        }
        if (usersChecked == 0) {
            LOG.info("[AbsenceJob] No users found with attendance history.");
        }
        LOG.info("========================================");
    }
}
