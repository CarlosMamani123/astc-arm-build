package com.backoffice.backoffice.infrastructure.repository;

import com.backoffice.backoffice.domain.entity.OutboxEventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepositoryBase<OutboxEventEntity, UUID> {

    private static final Logger LOG = Logger.getLogger(OutboxEventRepository.class);

    @SuppressWarnings("unchecked")
    public List<OutboxEventEntity> findUnprocessed(int limit) {
        List<OutboxEventEntity> results = getEntityManager().createNativeQuery(
                "SELECT * FROM outbox_events " +
                "WHERE processed_at IS NULL AND status = 'PENDING' " +
                "ORDER BY created_at ASC " +
                "LIMIT :limit FOR UPDATE SKIP LOCKED", OutboxEventEntity.class)
                .setParameter("limit", limit)
                .getResultList();
        
        if (!results.isEmpty()) {
            LOG.infof("[OUTBOX] Found %d unprocessed events with SKIP LOCKED in backoffice", results.size());
        }
        return results;
    }

    public void markPublishedBatch(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        getEntityManager().createNativeQuery(
                "UPDATE outbox_events SET processed_at = NOW(), status = 'PUBLISHED', last_error = NULL WHERE id = ANY(?1)")
                .setParameter(1, ids.toArray(new UUID[0]))
                .executeUpdate();
    }
}