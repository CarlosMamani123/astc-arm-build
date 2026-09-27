package com.smms.assistance.infrastructure.controller.graphql;

import com.smms.assistance.application.in.GetDashboardPort;
import com.smms.assistance.application.in.ListAbsencesPort;
import com.smms.assistance.application.in.ListAttendancePort;
import com.smms.assistance.application.in.ListJustificationsPort;

import com.smms.assistance.infrastructure.controller.graphql.dto.*;

import com.smms.assistance.infrastructure.client.ProjectOutput;
import com.smms.assistance.infrastructure.security.AuthContext;
import com.smms.assistance.shared.util.TimezoneService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import io.smallrye.common.annotation.Blocking;

import org.jboss.logging.Logger;

import io.quarkus.panache.common.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@GraphQLApi
@ApplicationScoped
@RolesAllowed({"TEAM_MEMBER", "PROJECT_MANAGER", "ADMIN"})
@Blocking
public class AssistanceQueryResolver {

    private static final Logger LOG =
            Logger.getLogger(AssistanceQueryResolver.class);

    @Inject
    AuthContext authContext;

    @Inject
    com.smms.assistance.infrastructure.repository.AttendanceRepository attendanceRepository;

    @Inject
    com.smms.assistance.infrastructure.repository.AbsenceRepository absenceRepository;

    @Inject
    com.smms.assistance.infrastructure.repository.JustificationRepository justificationRepository;

    @Inject
    GetDashboardPort getDashboardPort;

    @Inject
    ListAttendancePort listAttendancePort;

    @Inject
    ListAbsencesPort listAbsencesPort;

    @Inject
    ListJustificationsPort listJustificationsPort;

    @Inject
    com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper pmClientWrapper;

    @Inject
    com.smms.assistance.infrastructure.storage.S3StorageAdapter s3StorageAdapter;

    // =========================
    // GET MY PROJECTS
    // =========================
    @Query("getmyprojects")
    public java.util.List<com.smms.assistance.infrastructure.client.ProjectOutput> getMyProjects() {
        logSecurity("GetMyProjects");
        return pmClientWrapper.getMyProjects();
    }

    // =========================
    // GET DASHBOARD
    // =========================
    @Query("getDashboard")
    public DashboardOutput getDashboard(
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate,
            @Name("projectId") UUID projectId) {

        logSecurity("GetDashboard");

        UUID userId = authContext.getUserId();

        LOG.info("📌 USER ID: " + userId);

        LocalDate from =
                fromDate != null
                        ? LocalDate.parse(fromDate)
                        : null;

        LocalDate to =
                toDate != null
                        ? LocalDate.parse(toDate)
                        : null;

        DashboardOutput output =
                DtoMapper.toDashboardOutput(
                        getDashboardPort.execute(userId, projectId, from, to));

        LOG.info("✅ DASHBOARD LOADED");

        return output;
    }

    // =========================
    // LIST ATTENDANCE
    // =========================
    @Query("listAttendance")
    public AttendancePageOutput listAttendance(
            @Name("page") int page,
            @Name("size") int size,
            @Name("projectId") UUID projectId,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate,
            @Name("status") String status) {

        logSecurity("ListAttendance");

        UUID userId = authContext.getUserId();

        LOG.info("📌 USER ID: " + userId);

        AttendancePageOutput output = DtoMapper.toAttendancePage(
                listAttendancePort.execute(
                        userId,
                        page,
                        size,
                        projectId,
                        fromDate,
                        toDate,
                        status));

        if (output.getItems() != null) {
            for (AttendanceOutput item : output.getItems()) {
                if (item.getPhotoUrl() != null && !item.getPhotoUrl().isBlank()) {
                    item.setPhotoUrl(s3StorageAdapter.getPublicUrl(item.getPhotoUrl()));
                }
            }
        }

        return output;
    }

    // =========================
    // GET TODAY'S ATTENDANCE
    // =========================
    @Query("getTodayAttendance")
    @RolesAllowed({"TEAM_MEMBER", "PROJECT_MANAGER", "ADMIN"})
    public AttendanceOutput getTodayAttendance(@Name("projectId") UUID projectId) {
        UUID userId = authContext.getUserId();
        String timezone = resolveProjectTimezone(userId, projectId);
        java.time.LocalDate today = TimezoneService.todayAtZone(timezone);

        java.util.Optional<com.smms.assistance.domain.entity.AttendanceEntity> existing;
        if (projectId != null) {
            existing = attendanceRepository
                    .find("userId = ?1 and projectId = ?2 and date = ?3", userId, projectId, today)
                    .firstResultOptional();
            if (existing.isEmpty()) {
                existing = attendanceRepository
                        .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                        .firstResultOptional();
            }
        } else {
            existing = attendanceRepository
                    .find("userId = ?1 and projectId is null and date = ?2", userId, today)
                    .firstResultOptional();
        }

        if (existing.isEmpty()) return null;

        AttendanceOutput output = DtoMapper.toAttendanceOutput(existing.get());
        if (output.getPhotoUrl() != null && !output.getPhotoUrl().isBlank()) {
            output.setPhotoUrl(s3StorageAdapter.getPublicUrl(output.getPhotoUrl()));
        }
        return output;
    }

    // =========================
    // LIST ABSENCES
    // =========================
    @Query("listAbsences")
    public AbsencePageOutput listAbsences(
            @Name("page") int page,
            @Name("size") int size,
            @Name("projectId") UUID projectId,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate,
            @Name("type") String type,
            @Name("justified") Boolean justified) {

        logSecurity("ListAbsences");

        UUID userId = authContext.getUserId();

        LOG.info("📌 USER ID: " + userId);

        return DtoMapper.toAbsencePage(
                listAbsencesPort.execute(
                        userId,
                        page,
                        size,
                        projectId,
                        fromDate,
                        toDate,
                        type,
                        justified));
    }

    // =========================
    // LIST JUSTIFICATIONS
    // =========================
    @Query("listJustifications")
    public JustificationPageOutput listJustifications(
            @Name("page") int page,
            @Name("size") int size,
            @Name("status") String status,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate) {

        logSecurity("ListJustifications");

        UUID userId = authContext.getUserId();

        LOG.info("📌 USER ID: " + userId);

        JustificationPageOutput output = DtoMapper.toJustificationPage(
                listJustificationsPort.execute(
                        userId,
                        page,
                        size,
                        status,
                        fromDate,
                        toDate));

        if (output.getItems() != null) {
            for (JustificationOutput item : output.getItems()) {
                if (item.getDocumentUrl() != null && !item.getDocumentUrl().isBlank()) {
                    item.setDocumentUrl(s3StorageAdapter.getPublicUrl(item.getDocumentUrl()));
                }
            }
        }

        return output;
    }

    // =========================
    // SECURITY LOGGER
    // =========================
    private void logSecurity(String method) {

        LOG.info("====================================");
        LOG.info("🔥 ENTER: " + method);

        try {

            UUID userId = authContext.getUserId();

            LOG.info("🔐 JWT DETECTED");
            LOG.info("👤 USER ID: " + userId);

        } catch (Exception e) {

            LOG.error("❌ JWT NOT DETECTED");
            LOG.error(e.getMessage());
        }

        LOG.info("====================================");
    }

    // ==========================================
    // SECURITY VALIDATION (IDOR PATCH)
    // ==========================================
    private void validatePmOwnership(UUID requestedProjectId) {
        String role = authContext.getRole();
        if ("ADMIN".equals(role)) {
            System.out.println("🛡️ [Security] ADMIN bypass granted for projectId: " + requestedProjectId);
            return; // Admins can access all projects
        }
        if ("PROJECT_MANAGER".equals(role)) {
            if (requestedProjectId == null) {
                System.out.println("⚠️ [Security] PM accessing global view (projectId is null). Scoping to PM projects.");
                return;
            }
            try {
                java.util.List<ProjectOutput> projects = pmClientWrapper.getMyProjects();
                if (projects == null || projects.stream().noneMatch(p -> p.id != null && p.id.equals(requestedProjectId))) {
                    System.out.println("🚨 [Security IDOR Blocked] PM " + authContext.getUserId() + " attempted to access unauthorized projectId: " + requestedProjectId);
                    throw new SecurityException("Access Denied: You do not own or manage this project");
                }
                System.out.println("✅ [Security] PM Ownership validated for project: " + requestedProjectId);
            } catch (Exception e) {
                LOG.error("Failed to validate PM ownership: " + e.getMessage());
                throw new SecurityException("Access Denied: Could not verify project ownership");
            }
        }
    }

    private java.util.List<UUID> resolveAllowedProjectIds(UUID requestedProjectId) {
        String role = authContext.getRole();
        if ("ADMIN".equals(role)) {
            return requestedProjectId != null ? java.util.List.of(requestedProjectId) : null;
        }
        if ("PROJECT_MANAGER".equals(role)) {
            if (requestedProjectId != null) {
                validatePmOwnership(requestedProjectId);
                return java.util.List.of(requestedProjectId);
            }
            try {
                java.util.List<ProjectOutput> projects = pmClientWrapper.getMyProjects();
                if (projects != null && !projects.isEmpty()) {
                    return projects.stream()
                            .map(p -> p.id)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.toList());
                }
                return java.util.List.of();
            } catch (Exception e) {
                LOG.error("Failed to resolve PM project IDs: " + e.getMessage());
                return java.util.List.of();
            }
        }
        return requestedProjectId != null ? java.util.List.of(requestedProjectId) : null;
    }

    // ==========================================
    // PROJECT MANAGER ENDPOINTS
    // ==========================================

    @Query("listTeamAttendancePM")
    @RolesAllowed({"PROJECT_MANAGER", "ADMIN"})
    public AttendancePageOutput listTeamAttendancePM(
            @Name("userIds") java.util.List<UUID> userIds,
            @Name("projectId") UUID projectId,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate,
            @Name("status") String status,
            @Name("page") int page,
            @Name("size") int size
    ) {
        UUID currentPmId = authContext.getUserId();
        System.out.println("[Assistance PM API] [Query] ListTeamAttendancePM resolver execution flow starting. Initiated by PM: " + currentPmId 
                + " -> userIds count: " + (userIds != null ? userIds.size() : 0) 
                + ", projectId: " + projectId 
                + ", fromDate: " + fromDate 
                + ", toDate: " + toDate 
                + ", status: " + status 
                + ", page: " + page 
                + ", size: " + size);

        java.util.List<UUID> allowedProjectIds = resolveAllowedProjectIds(projectId);
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) {
            return new AttendancePageOutput(java.util.List.of(), page, size, 0L);
        }

        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        com.smms.assistance.domain.model.Page<com.smms.assistance.domain.entity.AttendanceEntity> pageData = 
                attendanceRepository.findPageByUsersNative(userIds, projectId, allowedProjectIds, from, to, status, page, size);

        java.util.List<com.smms.assistance.domain.entity.AttendanceEntity> items = pageData.getItems();
        long total = pageData.getTotalItems();

        long photosCount = items.stream().filter(a -> a.getPhotoUrl() != null && !a.getPhotoUrl().isBlank()).count();
        System.out.println("[Assistance PM API] [Query] ListTeamAttendancePM matched " + items.size() + " records out of " + total + " total. Photos: " + photosCount);

        AttendancePageOutput output = DtoMapper.toAttendancePage(pageData);

        if (output.getItems() != null) {
            for (AttendanceOutput item : output.getItems()) {
                if (item.getPhotoUrl() != null && !item.getPhotoUrl().isBlank()) {
                    item.setPhotoUrl(s3StorageAdapter.getPublicUrl(item.getPhotoUrl()));
                }
            }
        }

        System.out.println("[Assistance PM API] [Query] ListTeamAttendancePM resolved successfully and returning response page");

        return output;
    }

    @Query("listTeamJustificationsPM")
    @RolesAllowed({"PROJECT_MANAGER", "ADMIN"})
    public JustificationPageOutput listTeamJustificationsPM(
            @Name("userIds") java.util.List<UUID> userIds,
            @Name("status") String status,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate,
            @Name("page") int page,
            @Name("size") int size
    ) {
        UUID currentPmId = authContext.getUserId();
        System.out.println("[Assistance PM API] [Query] ListTeamJustificationsPM initiated by PM: " + currentPmId 
                + " -> userIds count: " + (userIds != null ? userIds.size() : 0) 
                + ", status: " + status 
                + ", fromDate: " + fromDate 
                + ", toDate: " + toDate 
                + ", page: " + page 
                + ", size: " + size);

        java.util.List<UUID> allowedProjectIds = resolveAllowedProjectIds(null);
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) {
            return new JustificationPageOutput(java.util.List.of(), page, size, 0L);
        }

        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        com.smms.assistance.domain.model.Page<com.smms.assistance.domain.entity.JustificationEntity> pageData = 
                justificationRepository.findPageByUsersNative(userIds, allowedProjectIds, status, from, to, page, size);

        java.util.List<com.smms.assistance.domain.entity.JustificationEntity> items = pageData.getItems();
        long total = pageData.getTotalItems();

        System.out.println("[Assistance PM API] [Query] ListTeamJustificationsPM matched " + items.size() + " records out of " + total + " total.");

        JustificationPageOutput output = DtoMapper.toJustificationPage(pageData);

        if (output.getItems() != null) {
            for (JustificationOutput item : output.getItems()) {
                if (item.getDocumentUrl() != null && !item.getDocumentUrl().isBlank()) {
                    item.setDocumentUrl(s3StorageAdapter.getPublicUrl(item.getDocumentUrl()));
                }
            }
        }

        return output;
    }

    private static String formatDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalDateTime) return val.toString();
        if (val instanceof java.time.OffsetDateTime) return ((java.time.OffsetDateTime) val).toLocalDateTime().toString();
        if (val instanceof java.time.Instant) return ((java.time.Instant) val).toString();
        if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime().toString();
        if (val instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) val).getTime()).toLocalDateTime().toString();
        }
        return val.toString();
    }

    @Query("getJustificationDetailPM")
    public JustificationOutput getJustificationDetailPM(@Name("id") UUID id) {
        UUID currentUserId = authContext.getUserId();
        String currentRole = authContext.getRole();
        System.out.println("[Assistance PM API] [Query] GetJustificationDetailPM initiated by user: " + currentUserId + " (role: " + currentRole + ") -> justificationId: " + id);

        Object[] row = justificationRepository.findDetailWithAbsenceNative(id);
        if (row == null) {
            throw new IllegalArgumentException("Justification not found");
        }

        UUID userId = row[1] != null ? UUID.fromString(row[1].toString()) : null;
        if ("TEAM_MEMBER".equals(currentRole) && !currentUserId.equals(userId)) {
            throw new SecurityException("Access denied: justification does not belong to you");
        }

        JustificationOutput output = new JustificationOutput();
        output.setId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
        output.setUserId(userId);
        output.setAbsenceId(row[2] != null ? UUID.fromString(row[2].toString()) : null);
        output.setDescription((String) row[3]);
        
        String docUrl = (String) row[4];
        if (docUrl != null && !docUrl.isBlank()) {
            output.setDocumentUrl(s3StorageAdapter.getPublicUrl(docUrl));
        } else {
            output.setDocumentUrl(null);
        }

        output.setStatus((String) row[5]);
        output.setComment((String) row[6]);
        
        if (row[7] != null) output.setSubmittedAt(formatDateTime(row[7]));
        if (row[8] != null) output.setReviewedAt(formatDateTime(row[8]));
        output.setReviewedBy(row[9] != null ? UUID.fromString(row[9].toString()) : null);

        // Enrichment with absence info (from LEFT JOIN, no extra DB query!)
        if (row[12] != null) {
            output.setAbsenceDate(row[12].toString());
        }
        output.setAbsenceType((String) row[13]);

        System.out.println("[Assistance PM API] [Query] GetJustificationDetailPM enriched with absence info -> absenceDate: " + output.getAbsenceDate() + ", absenceType: " + output.getAbsenceType());

        var historyEntities = com.smms.assistance.domain.entity.JustificationHistoryEntity
                .find("justificationId = ?1", io.quarkus.panache.common.Sort.by("changedAt").ascending(), id)
                .<com.smms.assistance.domain.entity.JustificationHistoryEntity>list();

        var historyOutputs = historyEntities.stream()
                .map(h -> JustificationHistoryOutput.builder()
                        .id(h.getId())
                        .justificationId(h.getJustificationId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .comment(h.getComment())
                        .changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt() != null ? DtoMapper.utcToLima(h.getChangedAt()) : null)
                        .build())
                .collect(Collectors.toList());

        output.setHistory(historyOutputs);

        return output;
    }

    @Inject
    EntityManager entityManager;

    @Query("getDashboardPMInternal")
    @RolesAllowed({"PROJECT_MANAGER", "ADMIN"})
    public DashboardOutput getDashboardPMInternal(
            @Name("userIds") java.util.List<UUID> userIds,
            @Name("projectId") UUID projectId,
            @Name("fromDate") String fromDate,
            @Name("toDate") String toDate
    ) {
        UUID currentPmId = authContext.getUserId();
        System.out.println("[Assistance PM API] [Query] GetDashboardPMInternal initiated by PM: " + currentPmId 
                + " -> userIds count: " + (userIds != null ? userIds.size() : 0) 
                + ", projectId: " + projectId 
                + ", fromDate: " + fromDate 
                + ", toDate: " + toDate);

        java.util.List<UUID> allowedProjectIds = resolveAllowedProjectIds(projectId);
        if (allowedProjectIds != null && allowedProjectIds.isEmpty()) {
            return DashboardOutput.builder()
                    .totalAttendances(0)
                    .totalAbsences(0)
                    .pendingJustifications(0)
                    .build();
        }

        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        int totalAttendances = 0;
        int totalAbsences = 0;
        int pendingJustifications = 0;

        if (userIds != null && !userIds.isEmpty()) {
            StringBuilder sql = new StringBuilder("SELECT ");

            // 1. Attendances
            sql.append("(SELECT COUNT(id) FROM attendances WHERE user_id IN :userIds AND status NOT IN ('OUTSIDE_SCHEDULE', 'OUTSIDE_SCHEDULE_CHECKED_OUT')");
            if (allowedProjectIds != null) {
                sql.append(" AND project_id IN :allowedProjectIds");
            }
            if (from != null) {
                sql.append(" AND date >= :fromDate");
            }
            if (to != null) {
                sql.append(" AND date <= :toDate");
            }
            sql.append("), ");

            // 2. Absences
            sql.append("(SELECT COUNT(id) FROM absences WHERE user_id IN :userIds");
            if (allowedProjectIds != null) {
                sql.append(" AND project_id IN :allowedProjectIds");
            }
            if (from != null) {
                sql.append(" AND date >= :fromDate");
            }
            if (to != null) {
                sql.append(" AND date <= :toDate");
            }
            sql.append("), ");

            // 3. Justifications
            sql.append("(SELECT COUNT(j.id) FROM justifications j JOIN absences a ON a.id = j.absence_id WHERE j.user_id IN :userIds AND j.status IN ('SUBMITTED', 'PENDING')");
            if (allowedProjectIds != null) {
                sql.append(" AND a.project_id IN :allowedProjectIds");
            }
            if (from != null) {
                sql.append(" AND a.date >= :fromDate");
            }
            if (to != null) {
                sql.append(" AND a.date <= :toDate");
            }
            sql.append(")");

            var nativeQuery = entityManager.createNativeQuery(sql.toString())
                    .setParameter("userIds", userIds);
            if (allowedProjectIds != null) {
                nativeQuery.setParameter("allowedProjectIds", allowedProjectIds);
            }
            if (from != null) {
                nativeQuery.setParameter("fromDate", from);
            }
            if (to != null) {
                nativeQuery.setParameter("toDate", to);
            }

            Object[] result = (Object[]) nativeQuery.getSingleResult();

            totalAttendances = ((Number) result[0]).intValue();
            totalAbsences = ((Number) result[1]).intValue();
            pendingJustifications = ((Number) result[2]).intValue();
        }

        System.out.println("[Assistance PM API] [Query] GetDashboardPMInternal summary -> Attendances: " + totalAttendances 
                + ", Absences: " + totalAbsences 
                + ", Pending Justifications: " + pendingJustifications);

        return DashboardOutput.builder()
                .totalAttendances(totalAttendances)
                .totalAbsences(totalAbsences)
                .pendingJustifications(pendingJustifications)
                .build();
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
            System.out.println("[Query] Could not resolve project timezone, using default");
        }
        return TimezoneService.defaultTimezone();
    }
}