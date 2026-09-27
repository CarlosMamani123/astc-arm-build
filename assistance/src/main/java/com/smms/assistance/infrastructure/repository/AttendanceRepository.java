package com.smms.assistance.infrastructure.repository;

import com.smms.assistance.domain.entity.AttendanceEntity;
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
public class AttendanceRepository implements PanacheRepositoryBase<AttendanceEntity, UUID> {

    public List<AttendanceEntity> findByUser(
            UUID userId,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {

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
        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        return find(query.toString(), Sort.by("date").descending(), params)
                .page(io.quarkus.panache.common.Page.of(page, size))
                .list();
    }

    public long countByUser(
            UUID userId,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status) {

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
        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        return count(query.toString(), params);
    }

    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }

    public List<AttendanceEntity> findRecentByUser(UUID userId, int limit) {
        return find("userId = :userId", Sort.by("date").descending(),
                Map.of("userId", userId))
                .page(io.quarkus.panache.common.Page.of(0, limit))
                .list();
    }

    public List<AttendanceEntity> findByUsers(
            List<UUID> userIds,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status,
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
        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        return find(query.toString(), Sort.by("date").descending(), params)
                .page(io.quarkus.panache.common.Page.of(page, size))
                .list();
    }

    public long countByUsers(
            List<UUID> userIds,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status) {

        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (userIds != null && !userIds.isEmpty()) {
            query.append(" and userId in :userIds");
            params.put("userIds", userIds);
        } else {
            return 0;
        }

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
        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        return count(query.toString(), params);
    }
    
    // NEW NATIVE QUERY IMPLEMENTATIONS
    public Page<AttendanceEntity> findPageByUserNative(
            UUID userId,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {
            
        long oldStart = System.currentTimeMillis();
        findByUser(userId, projectId, from, to, status, page, size);
        countByUser(userId, projectId, from, to, status);
        long oldTime = System.currentTimeMillis() - oldStart;

        long newStart = System.currentTimeMillis();
        
        StringBuilder sql = new StringBuilder(
            "SELECT CAST(a.id AS VARCHAR), CAST(a.user_id AS VARCHAR), CAST(a.project_id AS VARCHAR), " +
            "a.date, a.check_in, a.check_out, a.status, a.latitude, a.longitude, " +
            "a.photo_url, a.project_timezone, a.created_at, a.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM attendances a WHERE a.user_id = :userId"
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
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = :status");
            params.put("status", status);
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
        System.out.println(String.format("[PERF] findPageByUserNative (attendance) -> JPQL (2 queries): %d ms | Native (1 query OVER): %d ms", oldTime, newTime));

        long totalCount = 0;
        List<AttendanceEntity> mapped = new ArrayList<>();
        
        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[13]).longValue();
            }
            AttendanceEntity entity = new AttendanceEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setProjectId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setDate(toLocalDate(row[3]));
            entity.setCheckIn(toLocalTime(row[4]));
            entity.setCheckOut(toLocalTime(row[5]));
            
            entity.setStatus((String) row[6]);
            if (row[7] != null) entity.setLatitude(((Number) row[7]).doubleValue());
            if (row[8] != null) entity.setLongitude(((Number) row[8]).doubleValue());
            entity.setPhotoUrl((String) row[9]);
            entity.setProjectTimezone((String) row[10]);
            
            entity.setCreatedAt(toLocalDateTime(row[11]));
            entity.setUpdatedAt(toLocalDateTime(row[12]));
            
            mapped.add(entity);
        }
        
        return new Page<>(mapped, totalCount, page, size);
    }
    
    public Page<AttendanceEntity> findPageByUsersNative(
            List<UUID> userIds,
            UUID projectId,
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {
        return findPageByUsersNative(userIds, projectId, null, from, to, status, page, size);
    }

    public Page<AttendanceEntity> findPageByUsersNative(
            List<UUID> userIds,
            UUID projectId,
            List<UUID> allowedProjectIds,
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {
            
        long oldStart = System.currentTimeMillis();
        long newStart = System.currentTimeMillis();
        
        StringBuilder sql = new StringBuilder(
            "SELECT CAST(a.id AS VARCHAR), CAST(a.user_id AS VARCHAR), CAST(a.project_id AS VARCHAR), " +
            "a.date, a.check_in, a.check_out, a.status, a.latitude, a.longitude, " +
            "a.photo_url, a.project_timezone, a.created_at, a.updated_at, " +
            "COUNT(*) OVER() " +
            "FROM attendances a WHERE 1=1"
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
        } else if (allowedProjectIds != null) {
            if (allowedProjectIds.isEmpty()) {
                sql.append(" AND 1=0");
            } else {
                sql.append(" AND a.project_id IN (:allowedProjectIds)");
                params.put("allowedProjectIds", allowedProjectIds);
            }
        }
        if (from != null) {
            sql.append(" AND a.date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            sql.append(" AND a.date <= :to");
            params.put("to", to);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = :status");
            params.put("status", status);
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
        System.out.println(String.format("[PERF] findPageByUsersNative (attendance) -> Native (1 query OVER): %d ms", newTime));

        long totalCount = 0;
        List<AttendanceEntity> mapped = new ArrayList<>();
        
        for (Object[] row : rows) {
            if (totalCount == 0) {
                totalCount = ((Number) row[13]).longValue();
            }
            AttendanceEntity entity = new AttendanceEntity();
            entity.setId(row[0] != null ? java.util.UUID.fromString(row[0].toString()) : null);
            entity.setUserId(row[1] != null ? java.util.UUID.fromString(row[1].toString()) : null);
            entity.setProjectId(row[2] != null ? java.util.UUID.fromString(row[2].toString()) : null);
            
            entity.setDate(toLocalDate(row[3]));
            entity.setCheckIn(toLocalTime(row[4]));
            entity.setCheckOut(toLocalTime(row[5]));
            
            entity.setStatus((String) row[6]);
            if (row[7] != null) entity.setLatitude(((Number) row[7]).doubleValue());
            if (row[8] != null) entity.setLongitude(((Number) row[8]).doubleValue());
            entity.setPhotoUrl((String) row[9]);
            entity.setProjectTimezone((String) row[10]);
            
            entity.setCreatedAt(toLocalDateTime(row[11]));
            entity.setUpdatedAt(toLocalDateTime(row[12]));
            
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

    private static java.time.LocalTime toLocalTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalTime) return (java.time.LocalTime) val;
        if (val instanceof java.sql.Time) return ((java.sql.Time) val).toLocalTime();
        return java.time.LocalTime.parse(val.toString());
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
