package com.smms.assistance.infrastructure.messaging;

import com.smms.assistance.domain.entity.OutboxEventEntity;
import com.smms.assistance.infrastructure.repository.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class OutboxEventPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxEventPublisher.class);

    @Inject
    OutboxRepository outboxRepository;

    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, String payload) {
        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .published(false)
                .build();
        outboxRepository.persist(event);
        LOG.infof("[OUTBOX] Event persisted: id=%s, type=%s, aggregateType=%s, aggregateId=%s",
                event.getId(), eventType, aggregateType, aggregateId);
    }
}
