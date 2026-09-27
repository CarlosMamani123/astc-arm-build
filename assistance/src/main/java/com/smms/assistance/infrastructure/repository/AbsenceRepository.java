package com.smms.assistance.infrastructure.repository;

import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.model.Page;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@ApplicationScoped
public class AbsenceRepository implements PanacheRepositoryBase<AbsenceEntity, UUID> {

    // OLD METHODS KEPT FOR TIMING COMPARISON
    public List<AbsenceEntity> findByUser(UUID userId, UUID projectId, LocalDate from, LocalDate to,
                                          String type, Boolean justified,
                                          int page, int size) {
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (projectId != null) {
            query.append(" and projectId = :projectId");
            params.put("projectId", projectId);
        }
        if (from != null) {
            query.append(" and date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            query.append(" and date <= :to");
            params.put("to", to);
        }
        if (type != null && !type.isBlank()) {
            query.append(" and type = :type");
            params.put("type", type);
        }
        if (justified != null) {
            query.append(" and justified = :justified");
            params.put("justified", justified);
        }

        return find(query.toString(), Sort.by("date").descending(), params)
                .page(io.quarkus.panache.common.Page.of(page, size))
                .list();
    }

    public long countByUser(UUID userId, UUID projectId, LocalDate from, LocalDate to,
                            String type, Boolean justified) {
        StringBuilder query = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (projectId != null) {
            query.append(" and projectId = :projectId");
            params.put("projectId", projectId);
        }
        if (from != null) {
            query.append(" and date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            query.append(" and date <= :to");
            params.put("to", to);
        }
        if (type != null && !type.isBlank()) {
            query.append(" and type = :type");
            params.put("type", type);
        }
        if (justified != null) {
            query.append(" and justified = :justified");
            params.put("justified", justified);
        }

        return count(query.toString(), params);
    }
    
    // NATIVE QUERY - FIND BY USER
    public Page<AbsenceEntity> findPageByUserNative(UUID userId, UUID projectId, LocalDate from, LocalDate to,
                                                    String type, Boolean justified, int page, int size) {
        long oldStart = System.currentTimeMillis();
        findByUser(userId, projectId, from, to, type, justified, page, size);
        countByUser(userId, projectId, from, to, type, justified);
        long oldTime = System.currentTimeMillis() - oldStart;

        long newStart = System.currentTimeMillis();

        StringBuilder sql = new StringBuilder(
            "SELECT CAST(a.id AS VARCHAR), CAST(a.user_id AS VARCHAR), CAST(a.project_id AS VARCHAR), " +
            "a.date, a.type, a.justified, a.created_at, a.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM absences a WHERE a.user_id = :userId"
        );
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (projectId != null) {
            sql.append(" AND a.project_id = :projectId");
            params.put("projectId", projectId);
        }
        if (from != null) {
            sql.append(" AND a.date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            sql.append(" AND a.date <= :to");
            params.put("to", to);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND a.type = :type");
            params.put("type", type);
        }
        if (justified != null) {
            sql.append(" AND a.justified = :justified");
            params.put("justified", justified);
        }

        sql.append(" ORDER BY a.date DESC LIMIT :limit OFFSET :offset");
        params.put("limit", size);
        params.put("offset", Math.max(0, page - 1) * size);

        var nativeQuery = getEntityManager().createNativeQuery(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        long newTime = System.currentTimeMillis() - newStart;
        System.out.println(String.format("[PERF] findPageByUserNative (absences) -> JPQL (2 queries): %d ms | Native (1 query OVER): %d ms", oldTime, newTime));

        long totalCount = 0;
        List<AbsenceEntity> mapped = new ArrayList<>();

        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[8]).longValue();
            }
            AbsenceEntity entity = new AbsenceEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setProjectId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setDate(toLocalDate(row[3]));
            entity.setType((String) row[4]);
            if (row[5] != null) entity.setJustified((Boolean) row[5]);
            
            entity.setCreatedAt(toLocalDateTime(row[6]));
            entity.setUpdatedAt(toLocalDateTime(row[7]));
            
            mapped.add(entity);
        }
        
        return new Page<>(mapped, totalCount, page, size);
    }
    
    // NATIVE QUERY - FIND BY USERS
    public Page<AbsenceEntity> findPageByUsersNative(List<UUID> userIds, UUID projectId, LocalDate from, LocalDate to,
                                                     String type, Boolean justified, int page, int size) {
        StringBuilder sql = new StringBuilder(
            "SELECT CAST(a.id AS VARCHAR), CAST(a.user_id AS VARCHAR), CAST(a.project_id AS VARCHAR), " +
            "a.date, a.type, a.justified, a.created_at, a.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM absences a WHERE 1=1"
        );
        Map<String, Object> params = new HashMap<>();

        if (userIds != null) {
            if (!userIds.isEmpty()) {
                sql.append(" AND a.user_id IN (:userIds)");
                params.put("userIds", userIds);
            } else {
                sql.append(" AND 1=0");
            }
        }

        if (projectId != null) {
            sql.append(" AND a.project_id = :projectId");
            params.put("projectId", projectId);
        }
        if (from != null) {
            sql.append(" AND a.date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            sql.append(" AND a.date <= :to");
            params.put("to", to);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND a.type = :type");
            params.put("type", type);
        }
        if (justified != null) {
            sql.append(" AND a.justified = :justified");
            params.put("justified", justified);
        }

        sql.append(" ORDER BY a.date DESC LIMIT :limit OFFSET :offset");
        params.put("limit", size);
        params.put("offset", Math.max(0, page - 1) * size);

        var nativeQuery = getEntityManager().createNativeQuery(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            nativeQuery.setParameter(entry.getKey(), entry.getValue());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        long totalCount = 0;
        List<AbsenceEntity> mapped = new ArrayList<>();

        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[8]).longValue();
            }
            AbsenceEntity entity = new AbsenceEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setProjectId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setDate(toLocalDate(row[3]));
            entity.setType((String) row[4]);
            if (row[5] != null) entity.setJustified((Boolean) row[5]);
            
            entity.setCreatedAt(toLocalDateTime(row[6]));
            entity.setUpdatedAt(toLocalDateTime(row[7]));
            
            mapped.add(entity);
        }
        
        return new Page<>(mapped, totalCount, page, size);
    }

    private static java.time.LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalDate) return (java.time.LocalDate) val;
        if (val instanceof java.sql.Date) return ((java.sql.Date) val).toLocalDate();
        if (val instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) val).getTime()).toLocalDate();
        }
        return java.time.LocalDate.parse(val.toString());
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
