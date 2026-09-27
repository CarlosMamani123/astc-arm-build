package com.backoffice.backoffice.application.usecase;

import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

public interface GetNotificationsUseCase {
    Object[] getByUserId(UUID userId, int page, int size);
    long countUnread(UUID userId);
    Object[] findByUserIdAndCategory(UUID userId, String category, int page, int size);
    long countUnreadByCategory(UUID userId, String category);
}