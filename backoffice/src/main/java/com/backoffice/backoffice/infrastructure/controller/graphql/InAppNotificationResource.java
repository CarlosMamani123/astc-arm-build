package com.backoffice.backoffice.infrastructure.controller.graphql;

import com.backoffice.backoffice.application.usecase.GetNotificationsUseCase;
import com.backoffice.backoffice.application.usecase.ReadNotificationUseCase;
import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import com.backoffice.backoffice.infrastructure.controller.graphql.dto.InAppNotificationCategoryUnreadCountResponse;
import com.backoffice.backoffice.infrastructure.controller.graphql.dto.InAppNotificationResponse;
import com.backoffice.backoffice.infrastructure.controller.graphql.dto.PaginationResponse;
import com.backoffice.backoffice.infrastructure.security.AuthContext;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@GraphQLApi
public class InAppNotificationResource {

    private static final Logger LOG = Logger.getLogger(InAppNotificationResource.class);

    @Inject
    GetNotificationsUseCase getNotificationsUseCase;

    @Inject
    ReadNotificationUseCase readNotificationUseCase;

    @Inject
    AuthContext authContext;

    @Query("getallnotifications")
    @Description("Get notifications for a user with pagination")
    @Blocking
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public PaginationResponse getNotifications(
            @Name("userId") String userId,
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("10") int size) {
        String effectiveUserId = userId;
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            try { effectiveUserId = authContext.getUserId().toString(); } catch (Exception e) { return new PaginationResponse(page, size, 0, new java.util.ArrayList<>()); }
        }
        LOG.infof("GraphQL Query GetAllNotifications: userId=%s, page=%d, size=%d", effectiveUserId, page, size);
        int internalPage = Math.max(page - 1, 0);
        Object[] result = getNotificationsUseCase.getByUserId(UUID.fromString(effectiveUserId), internalPage, size);
        List<InAppNotificationEntity> items = (List<InAppNotificationEntity>) result[0];
        long total = (Long) result[1];
        return new PaginationResponse(page, size, total,
                items.stream()
                        .map(InAppNotificationResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @Query("getUnreadCount")
    @Description("Get count of unread notifications for a user, optionally filtered by category")
    @Blocking
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public Long getUnreadCount(
            @Name("userId") String userId,
            @Name("category") @DefaultValue("") String category) {
        String effectiveUserId = userId;
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            try {
                effectiveUserId = authContext.getUserId().toString();
            } catch (Exception e) {
                LOG.warnf("Could not extract userId from JWT: %s", e.getMessage());
                return 0L;
            }
        }
        LOG.infof("GraphQL Query GetUnreadCount: userId=%s, category=%s", effectiveUserId, category);
        if (category == null || category.isBlank()) {
            return getNotificationsUseCase.countUnread(UUID.fromString(effectiveUserId));
        } else {
            return getNotificationsUseCase.countUnreadByCategory(UUID.fromString(effectiveUserId), category);
        }
    }

    @Query("getNotificationsByCategory")
    @Description("Get notifications for a user filtered by category with pagination")
    @Blocking
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public PaginationResponse getNotificationsByCategory(
            @Name("userId") String userId,
            @Name("category") String category,
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("10") int size) {
        String effectiveUserId = userId;
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            try { effectiveUserId = authContext.getUserId().toString(); } catch (Exception e) { return new PaginationResponse(page, size, 0, new java.util.ArrayList<>()); }
        }
        LOG.infof("GraphQL Query GetNotificationsByCategory: userId=%s, category=%s, page=%d, size=%d", effectiveUserId, category, page, size);
        int internalPage = Math.max(page - 1, 0);
        UUID userUUID = UUID.fromString(effectiveUserId);
        Object[] result = getNotificationsUseCase.findByUserIdAndCategory(userUUID, category, internalPage, size);
        List<InAppNotificationEntity> items = (List<InAppNotificationEntity>) result[0];
        long total = (Long) result[1];
        return new PaginationResponse(page, size, total,
                items.stream()
                        .map(InAppNotificationResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @Mutation("readNotification")
    @Description("Get notification detail and mark it as read if it was unread")
    @Blocking
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public InAppNotificationResponse readNotification(
            @Name("notificationId") String notificationId,
            @Name("userId") String userId) {
        LOG.infof("GraphQL Mutation ReadNotification: notificationId=%s, userId=%s", notificationId, userId);
        InAppNotificationEntity entity = readNotificationUseCase.readNotification(
                UUID.fromString(notificationId), UUID.fromString(userId));
        return InAppNotificationResponse.fromEntity(entity);
    }

    @Mutation("deleteNotification")
    @Description("Delete a notification by id for a specific user")
    @Blocking
    @jakarta.transaction.Transactional
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public Boolean deleteNotification(
            @Name("notificationId") String notificationId,
            @Name("userId") String userId) {
        LOG.infof("GraphQL Mutation DeleteNotification: notificationId=%s, userId=%s", notificationId, userId);
        try {
            boolean deleted = inAppNotificationRepository.deleteByIdAndUserId(
                    UUID.fromString(notificationId), UUID.fromString(userId));
            if (deleted) {
                inAppNotificationRepository.getEntityManager().flush();
            }
            return deleted;
        } catch (Exception e) {
            LOG.errorf(e, "Error deleting notification %s for user %s", notificationId, userId);
            return false;
        }
    }

    @Mutation("markAllNotificationsAsRead")
    @Description("Mark all unread notifications as read for a specific user")
    @Blocking
    @jakarta.transaction.Transactional
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public Boolean markAllNotificationsAsRead(@Name("userId") String userId) {
        LOG.infof("GraphQL Mutation MarkAllNotificationsAsRead: userId=%s", userId);
        try {
            int updated = inAppNotificationRepository.markAllAsReadByUserId(UUID.fromString(userId));
            LOG.infof("Marked %d notifications as read for user %s", updated, userId);
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Error marking all notifications as read for user %s", userId);
            return false;
        }
    }

    @Inject
    com.backoffice.backoffice.infrastructure.repository.InAppNotificationRepository inAppNotificationRepository;

    @Inject
    com.backoffice.backoffice.infrastructure.repository.DeviceTokenRepository deviceTokenRepository;

    @Query("getUnreadCountByCategory")
    @Description("Get unread notification count for each category for a user")
    @Blocking
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public List<InAppNotificationCategoryUnreadCountResponse> getUnreadCountByCategory(
            @Name("userId") String userId) {
        LOG.infof("GraphQL Query GetUnreadCountByCategory: userId=%s", userId);
        UUID userUUID = UUID.fromString(userId);
        List<Object[]> rows = inAppNotificationRepository.countUnreadGroupedByCategory(userUUID);
        List<InAppNotificationCategoryUnreadCountResponse> list = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            list.add(InAppNotificationCategoryUnreadCountResponse.builder()
                    .category((String) row[0])
                    .count(((Number) row[1]).longValue())
                    .build());
        }
        return list;
    }


    @Mutation("registerDevice")
    @Description("Register or update a user's device token for push notifications")
    @Blocking
    @jakarta.transaction.Transactional
    @RolesAllowed({"ADMIN", "PROJECT_MANAGER", "TEAM_MEMBER"})
    public Boolean registerDevice(
            @Name("userId") String userId,
            @Name("token") String token,
            @Name("platform") String platform) {
        LOG.infof("GraphQL Mutation RegisterDevice: userId=%s, platform=%s", userId, platform);
        UUID userUUID = UUID.fromString(userId);
        
        long start = System.currentTimeMillis();
        deviceTokenRepository.getEntityManager().createNativeQuery(
            "INSERT INTO notification.device_token (id, user_id, token, platform, created_at) " +
            "VALUES (:id, :userId, :token, :platform, NOW()) " +
            "ON CONFLICT (token) DO UPDATE SET " +
            "user_id = EXCLUDED.user_id, " +
            "platform = EXCLUDED.platform")
            .setParameter("id", UUID.randomUUID())
            .setParameter("userId", userUUID)
            .setParameter("token", token)
            .setParameter("platform", platform.toUpperCase())
            .executeUpdate();
            
        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] registerDevice (Native UPSERT) -> duration: " + duration + " ms");
        return true;
    }
}