package com.backoffice.backoffice.application.usecase;

import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import com.backoffice.backoffice.infrastructure.repository.InAppNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetNotificationsUseCaseImpl implements GetNotificationsUseCase {

    private static final Logger LOG = Logger.getLogger(GetNotificationsUseCaseImpl.class);

    @Inject
    InAppNotificationRepository repository;

    @Override
    public Object[] getByUserId(UUID userId, int page, int size) {
        LOG.infof("[NOTIFICATION] Fetching notifications for userId=%s, page=%d, size=%d", userId, page, size);
        return repository.findByUserIdWithCount(userId, page, size);
    }

    @Override
    public long countUnread(UUID userId) {
        long count = repository.countUnreadByUserId(userId);
        LOG.infof("[NOTIFICATION] Unread count for userId=%s: %d", userId, count);
        return count;
    }

    @Override
    public Object[] findByUserIdAndCategory(UUID userId, String category, int page, int size) {
        LOG.infof("[NOTIFICATION] Fetching notifications by category: userId=%s, category=%s, page=%d, size=%d", userId, category, page, size);
        return repository.findByUserIdAndCategoryWithCount(userId, category, page, size);
    }

    @Override
    public long countUnreadByCategory(UUID userId, String category) {
        long count = repository.countUnreadByUserIdAndCategory(userId, category);
        LOG.infof("[NOTIFICATION] Unread count by category: userId=%s, category=%s, count=%d", userId, category, count);
        return count;
    }
}