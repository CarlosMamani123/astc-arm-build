package com.smms.assistance.infrastructure.repository;

import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.model.Page;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
public class JustificationRepository implements PanacheRepositoryBase<JustificationEntity, UUID> {

    // OLD METHODS KEPT FOR TIMING COMPARISON
    public List<JustificationEntity> findByUser(UUID userId, String status,
            LocalDate from, LocalDate to,
            int page, int size) {
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        if (from != null) {
            query.append(" and COALESCE(submittedAt, createdAt) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and COALESCE(submittedAt, createdAt) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        return find(query.toString(), Sort.by("createdAt").descending(), params)
                .page(io.quarkus.panache.common.Page.of(page, size))
                .list();
    }

    public long countByUser(UUID userId, String status, LocalDate from, LocalDate to) {
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        if (from != null) {
            query.append(" and COALESCE(submittedAt, createdAt) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and COALESCE(submittedAt, createdAt) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        return count(query.toString(), params);
    }

    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }

    public List<JustificationEntity> findRecentByUser(UUID userId, int limit) {
        return find("userId = :userId", Sort.by("submittedAt").descending(),
                Map.of("userId", userId))
                .page(io.quarkus.panache.common.Page.of(0, limit))
                .list();
    }

    public Optional<JustificationEntity> findByAbsenceId(UUID absenceId) {
        return find("absenceId", absenceId).firstResultOptional();
    }

    public List<JustificationEntity> findByUsers(
            List<UUID> userIds,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (userIds != null && !userIds.isEmpty()) {
            query.append(" and userId in :userIds");
            params.put("userIds", userIds);
        } else {
            query.append(" and 1 = 0");
        }

        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        if (from != null) {
            query.append(" and COALESCE(submittedAt, createdAt) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and COALESCE(submittedAt, createdAt) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        return find(query.toString(), Sort.by("createdAt").descending(), params)
                .page(io.quarkus.panache.common.Page.of(page, size))
                .list();
    }

    public long countByUsers(
            List<UUID> userIds,
            String status,
            LocalDate from,
            LocalDate to) {

        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (userIds != null && !userIds.isEmpty()) {
            query.append(" and userId in :userIds");
            params.put("userIds", userIds);
        } else {
            return 0;
        }

        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        if (from != null) {
            query.append(" and COALESCE(submittedAt, createdAt) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and COALESCE(submittedAt, createdAt) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        return count(query.toString(), params);
    }
    
    // NATIVE QUERIES
    public Page<JustificationEntity> findPageByUserNative(UUID userId, String status,
            LocalDate from, LocalDate to,
            int page, int size) {
            
        long oldStart = System.currentTimeMillis();
        findByUser(userId, status, from, to, page, size);
        countByUser(userId, status, from, to);
        long oldTime = System.currentTimeMillis() - oldStart;

        long newStart = System.currentTimeMillis();

        StringBuilder sql = new StringBuilder(
            "SELECT CAST(j.id AS VARCHAR), CAST(j.user_id AS VARCHAR), CAST(j.absence_id AS VARCHAR), " +
            "j.status, j.description, j.submitted_at, j.reviewed_at, j.created_at, j.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM justifications j WHERE j.user_id = :userId"
        );
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (status != null) {
            sql.append(" AND j.status = :status");
            params.put("status", status);
        }
        if (from != null) {
            sql.append(" AND COALESCE(j.submitted_at, j.created_at) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND COALESCE(j.submitted_at, j.created_at) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        sql.append(" ORDER BY j.created_at DESC LIMIT :limit OFFSET :offset");
        params.put("limit", size);
        params.put("offset", Math.max(0, page - 1) * size);

        var nativeQuery = getEntityManager().createNativeQuery(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        long newTime = System.currentTimeMillis() - newStart;
        System.out.println(String.format("[PERF] findPageByUserNative (justifications) -> JPQL (2 queries): %d ms | Native (1 query OVER): %d ms", oldTime, newTime));

        long totalCount = 0;
        List<JustificationEntity> mapped = new ArrayList<>();

        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[9]).longValue();
            }
            JustificationEntity entity = new JustificationEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setAbsenceId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setStatus((String) row[3]);
            entity.setDescription((String) row[4]);
            
            entity.setSubmittedAt(toLocalDateTime(row[5]));
            entity.setReviewedAt(toLocalDateTime(row[6]));
            entity.setCreatedAt(toLocalDateTime(row[7]));
            entity.setUpdatedAt(toLocalDateTime(row[8]));
            
            mapped.add(entity);
        }
        
        return new Page<>(mapped, totalCount, page, size);
    }
    
    public Page<JustificationEntity> findPageByUsersNative(List<UUID> userIds, String status,
            LocalDate from, LocalDate to,
            int page, int size) {
        return findPageByUsersNative(userIds, null, status, from, to, page, size);
    }

    public Page<JustificationEntity> findPageByUsersNative(
            List<UUID> userIds,
            List<UUID> allowedProjectIds,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
            
        long oldStart = System.currentTimeMillis();
        long newStart = System.currentTimeMillis();

        StringBuilder sql = new StringBuilder(
            "SELECT CAST(j.id AS VARCHAR), CAST(j.user_id AS VARCHAR), CAST(j.absence_id AS VARCHAR), " +
            "j.status, j.description, j.submitted_at, j.reviewed_at, j.created_at, j.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM justifications j " +
            (allowedProjectIds != null ? "JOIN absences a ON a.id = j.absence_id " : "") +
            "WHERE 1=1"
        );
        Map<String, Object> params = new HashMap<>();

        if (userIds != null) {
            if (!userIds.isEmpty()) {
                sql.append(" AND j.user_id IN (:userIds)");
                params.put("userIds", userIds);
            } else {
                sql.append(" AND 1=0");
            }
        }

        if (allowedProjectIds != null) {
            if (allowedProjectIds.isEmpty()) {
                sql.append(" AND 1=0");
            } else {
                sql.append(" AND a.project_id IN (:allowedProjectIds)");
                params.put("allowedProjectIds", allowedProjectIds);
            }
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND j.status = :status");
            params.put("status", status);
        }
        if (from != null) {
            sql.append(" AND COALESCE(j.submitted_at, j.created_at) >= :from");
            params.put("from", from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND COALESCE(j.submitted_at, j.created_at) <= :to");
            params.put("to", to.plusDays(1).atStartOfDay());
        }

        sql.append(" ORDER BY j.created_at DESC LIMIT :limit OFFSET :offset");
        params.put("limit", size);
        params.put("offset", Math.max(0, page - 1) * size);

        var nativeQuery = getEntityManager().createNativeQuery(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        long newTime = System.currentTimeMillis() - newStart;
        System.out.println(String.format("[PERF] findPageByUsersNative (justifications) -> Native (1 query OVER): %d ms", newTime));

        long totalCount = 0;
        List<JustificationEntity> mapped = new ArrayList<>();

        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[9]).longValue();
            }
            JustificationEntity entity = new JustificationEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setAbsenceId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setStatus((String) row[3]);
            entity.setDescription((String) row[4]);
            
            entity.setSubmittedAt(toLocalDateTime(row[5]));
            entity.setReviewedAt(toLocalDateTime(row[6]));
            entity.setCreatedAt(toLocalDateTime(row[7]));
            entity.setUpdatedAt(toLocalDateTime(row[8]));
            
            mapped.add(entity);
        }
        
        return new Page<>(mapped, totalCount, page, size);
    }

    public Object[] findDetailWithAbsenceNative(UUID id) {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager().createNativeQuery(
                "SELECT j.id, j.user_id, j.absence_id, j.description, j.document_url, j.status, j.comment, " +
                "j.submitted_at, j.reviewed_at, j.reviewed_by, j.created_at, j.updated_at, " +
                "a.date AS absence_date, a.type AS absence_type " +
                "FROM justifications j " +
                "LEFT JOIN absences a ON a.id = j.absence_id " +
                "WHERE j.id = ?1"
        )
        .setParameter(1, id)
        .getResultList();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] getJustificationDetailPM (Native LEFT JOIN) -> duration: " + duration + " ms");

        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    private static java.time.LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) val;
        if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime();
        if (val instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) val).getTime()).toLocalDateTime();
        }
        return java.time.LocalDateTime.parse(val.toString());
    }
}
