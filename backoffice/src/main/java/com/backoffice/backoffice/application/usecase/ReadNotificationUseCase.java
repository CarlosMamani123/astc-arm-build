package com.backoffice.backoffice.application.usecase;

import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

public interface ReadNotificationUseCase {
    InAppNotificationEntity readNotification(UUID notificationId, UUID userId);
}