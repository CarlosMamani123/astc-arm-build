package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.GetDashboardPort;
import com.smms.assistance.domain.model.DashboardSummary;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
public class GetDashboardUseCase implements GetDashboardPort {

    @Inject
    AttendanceRepository attendanceRepository;

    @Inject
    AbsenceRepository absenceRepository;

    @Inject
    JustificationRepository justificationRepository;

    @Inject
    GenerateAbsenceIfNeededUseCase generateAbsenceIfNeededUseCase;

    @Inject
    EntityManager entityManager;

    @Override
    public DashboardSummary execute(UUID userId, UUID projectId, LocalDate fromDate, LocalDate toDate) {

        String projectFilter = projectId != null ? " AND project_id = :projectId" : "";
        String absProjectFilter = projectId != null ? " AND a.project_id = :projectId" : "";

        String sql = "SELECT " +
                "(SELECT COUNT(id) FROM attendances WHERE user_id = :userId AND status NOT IN ('OUTSIDE_SCHEDULE', 'OUTSIDE_SCHEDULE_CHECKED_OUT')" + projectFilter + " AND (CAST(:fromDate AS date) IS NULL OR date >= CAST(:fromDate AS date)) AND (CAST(:toDate AS date) IS NULL OR date <= CAST(:toDate AS date))) as att_count, " +
                "(SELECT COUNT(id) FROM absences WHERE user_id = :userId" + projectFilter + " AND (CAST(:fromDate AS date) IS NULL OR date >= CAST(:fromDate AS date)) AND (CAST(:toDate AS date) IS NULL OR date <= CAST(:toDate AS date))) as abs_count, " +
                "(SELECT COUNT(j.id) FROM justifications j LEFT JOIN absences a ON a.id = j.absence_id " +
                " WHERE j.user_id = :userId AND UPPER(j.status) IN ('PENDING', 'SUBMITTED', 'PENDIENTE', 'EN_REVISION')" + absProjectFilter +
                " AND (CAST(:fromDate AS date) IS NULL OR CAST(COALESCE(j.submitted_at, j.created_at, a.date) AS date) >= CAST(:fromDate AS date)) " +
                " AND (CAST(:toDate AS date) IS NULL OR CAST(COALESCE(j.submitted_at, j.created_at, a.date) AS date) <= CAST(:toDate AS date))) as just_count";

        var query = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate);
        if (projectId != null) {
            query.setParameter("projectId", projectId);
        }

        Object[] result = (Object[]) query.getSingleResult();

        return DashboardSummary.builder()
                .totalAttendances(((Number) result[0]).intValue())
                .totalAbsences(((Number) result[1]).intValue())
                .pendingJustifications(((Number) result[2]).intValue())
                .build();
    }
}