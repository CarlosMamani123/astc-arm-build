package com.backoffice.backoffice.infrastructure.repository;

import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class InAppNotificationRepository implements PanacheRepositoryBase<InAppNotificationEntity, UUID> {

    private static final Logger LOG = Logger.getLogger(InAppNotificationRepository.class);

    private static UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID) return (UUID) val;
        try {
            return UUID.fromString(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static java.time.Instant toInstant(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.Instant) return (java.time.Instant) val;
        if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toInstant();
        if (val instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) val).atZone(java.time.ZoneId.systemDefault()).toInstant();
        if (val instanceof java.time.OffsetDateTime) return ((java.time.OffsetDateTime) val).toInstant();
        if (val instanceof java.util.Date) return ((java.util.Date) val).toInstant();
        try {
            return java.time.Instant.parse(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        String s = val.toString().trim().toLowerCase();
        return s.equals("true") || s.equals("t") || s.equals("1");
    }

    @SuppressWarnings("unchecked")
    public Object[] findByUserIdWithCount(UUID userId, int page, int size) {
        long start = System.currentTimeMillis();
        List<Object[]> results = getEntityManager().createNativeQuery(
                "SELECT n.id, n.notification_code, n.title, n.body, n.user_id, n.read, n.created_at, n.read_at, COUNT(*) OVER() " +
                "FROM notification.in_app_notification n " +
                "WHERE n.user_id = :userId " +
                "ORDER BY n.created_at DESC")
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] findByUserIdWithCount (Native) -> duration: " + duration + " ms");
        
        long total = 0;
        List<InAppNotificationEntity> items = new java.util.ArrayList<>();
        for (Object[] row : results) {
            if (total == 0 && row[8] != null) total = ((Number) row[8]).longValue();
            InAppNotificationEntity e = new InAppNotificationEntity();
            e.setId(toUUID(row[0]));
            e.setNotificationCode((String) row[1]);
            e.setTitle((String) row[2]);
            e.setBody((String) row[3]);
            e.setUserId(toUUID(row[4]));
            e.setRead(toBoolean(row[5]));
            e.setCreatedAt(toInstant(row[6]));
            e.setReadAt(toInstant(row[7]));
            items.add(e);
        }
        return new Object[]{items, total};
    }

    public long countUnreadByUserId(UUID userId) {
        Number count = (Number) getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM notification.in_app_notification WHERE user_id = :userId AND read = false")
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue();
    }

    public int markAllAsReadByUserId(UUID userId) {
        return getEntityManager().createNativeQuery(
                "UPDATE notification.in_app_notification " +
                "SET read = true, read_at = :now " +
                "WHERE user_id = :userId AND read = false")
                .setParameter("now", java.sql.Timestamp.from(java.time.Instant.now()))
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public Object[] findByUserIdAndCategoryWithCount(UUID userId, String category, int page, int size) {
        long start = System.currentTimeMillis();
        List<Object[]> results = getEntityManager().createNativeQuery(
                "SELECT n.id, n.notification_code, n.title, n.body, n.user_id, n.read, n.created_at, n.read_at, COUNT(*) OVER() " +
                "FROM notification.in_app_notification n " +
                "JOIN notification.notification_definition d ON n.notification_code = d.code " +
                "WHERE n.user_id = :userId AND UPPER(d.category) = :category " +
                "ORDER BY n.created_at DESC")
                .setParameter("userId", userId)
                .setParameter("category", category.toUpperCase())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] findByUserIdAndCategoryWithCount (Native) -> duration: " + duration + " ms");
        
        long total = 0;
        List<InAppNotificationEntity> items = new java.util.ArrayList<>();
        for (Object[] row : results) {
            if (total == 0 && row[8] != null) total = ((Number) row[8]).longValue();
            InAppNotificationEntity e = new InAppNotificationEntity();
            e.setId(toUUID(row[0]));
            e.setNotificationCode((String) row[1]);
            e.setTitle((String) row[2]);
            e.setBody((String) row[3]);
            e.setUserId(toUUID(row[4]));
            e.setRead(toBoolean(row[5]));
            e.setCreatedAt(toInstant(row[6]));
            e.setReadAt(toInstant(row[7]));
            items.add(e);
        }
        return new Object[]{items, total};
    }

    public long countUnreadByUserIdAndCategory(UUID userId, String category) {
        long start = System.currentTimeMillis();
        Number count = (Number) getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM notification.in_app_notification n " +
                "JOIN notification.notification_definition d ON n.notification_code = d.code " +
                "WHERE n.user_id = :userId AND n.read = false AND UPPER(d.category) = :category")
                .setParameter("userId", userId)
                .setParameter("category", category.toUpperCase())
                .getSingleResult();
        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] countUnreadByUserIdAndCategory (Native) -> duration: " + duration + " ms");
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> countUnreadGroupedByCategory(UUID userId) {
        long start = System.currentTimeMillis();
        List<Object[]> results = getEntityManager().createNativeQuery(
                "SELECT d.category, COUNT(n.id) FROM notification.in_app_notification n " +
                "JOIN notification.notification_definition d ON n.notification_code = d.code " +
                "WHERE n.user_id = :userId AND n.read = false " +
                "GROUP BY d.category")
                .setParameter("userId", userId)
                .getResultList();
        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] countUnreadGroupedByCategory (Native) -> duration: " + duration + " ms");
        return results;
    }

    @jakarta.transaction.Transactional
    public boolean deleteByIdAndUserId(UUID id, UUID userId) {
        long deleted = getEntityManager().createNativeQuery(
                "DELETE FROM notification.in_app_notification WHERE id = :id AND user_id = :userId")
                .setParameter("id", id)
                .setParameter("userId", userId)
                .executeUpdate();
        return deleted > 0;
    }
}