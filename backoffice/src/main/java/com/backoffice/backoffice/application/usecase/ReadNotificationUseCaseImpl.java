package com.backoffice.backoffice.application.usecase;

import com.backoffice.backoffice.domain.notification.InAppNotificationEntity;
import com.backoffice.backoffice.infrastructure.repository.InAppNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ReadNotificationUseCaseImpl implements ReadNotificationUseCase {

    private static final Logger LOG = Logger.getLogger(ReadNotificationUseCaseImpl.class);

    @Inject
    InAppNotificationRepository repository;

    @Override
    @Transactional
    public InAppNotificationEntity readNotification(UUID notificationId, UUID userId) {
        long start = System.currentTimeMillis();
        try {
            InAppNotificationEntity entity = (InAppNotificationEntity) repository.getEntityManager().createNativeQuery(
                    "UPDATE notification.in_app_notification " +
                    "SET read = true, read_at = :now " +
                    "WHERE id = :id AND user_id = :userId " +
                    "RETURNING *", InAppNotificationEntity.class)
                    .setParameter("now", java.sql.Timestamp.from(Instant.now()))
                    .setParameter("id", notificationId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            
            long duration = System.currentTimeMillis() - start;
            System.out.println("[PERF] readNotification (Native RETURNING) -> duration: " + duration + " ms");
            return entity;
        } catch (jakarta.persistence.NoResultException e) {
            throw new SecurityException("Notification not found or does not belong to user");
        }
    }
}