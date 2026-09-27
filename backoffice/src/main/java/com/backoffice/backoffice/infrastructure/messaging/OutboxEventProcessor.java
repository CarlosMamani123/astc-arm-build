package com.backoffice.backoffice.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.backoffice.backoffice.domain.entity.OutboxEventEntity;
import com.backoffice.backoffice.infrastructure.repository.OutboxEventRepository;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OutboxEventProcessor {

    private static final Logger LOG = Logger.getLogger(OutboxEventProcessor.class);

    @Inject
    OutboxEventRepository repository;

    @Inject
    @Channel("notification-events")
    Emitter<String> notificationEmitter;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "event.outbox.batch-size", defaultValue = "50")
    int batchSize;

    @ConfigProperty(name = "event.outbox.max-attempts", defaultValue = "10")
    int maxAttempts;

    void init(@Observes StartupEvent ev) {
        vertx.setPeriodic(5000, id -> vertx.executeBlocking(
            promise -> {
                try {
                    io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
                        processEvents();
                    });
                    promise.complete();
                } catch (Exception e) {
                    LOG.errorf(e, "Error processing outbox events");
                    promise.fail(e);
                }
            },
            false
        ));
    }

    @ActivateRequestContext
    void processEvents() {
        List<OutboxEventEntity> events = repository.findUnprocessed(batchSize);
        if (events == null || events.isEmpty()) {
            return;
        }

        LOG.infof("📬 [Outbox] Found %d unprocessed outbox events to process in backoffice-service", events.size());

        List<java.util.UUID> successfulIds = new java.util.ArrayList<>();

        for (OutboxEventEntity event : events) {
            try {
                LOG.infof("📬 [Outbox] Processing event ID=%s, type=%s, payload=%s",
                        event.getId(), event.getEventType(), event.getPayload());
                JsonObject payload = new JsonObject(event.getPayload());
                String userId = payload.getString("id");

                String templateCode = mapEventType(event.getEventType());
                if (templateCode == null) {
                    LOG.warnf("⚠ [Outbox] Unknown event type '%s' for outbox event %s, marking as processed",
                            event.getEventType(), event.getId());
                    successfulIds.add(event.getId());
                    continue;
                }

                JsonObject variables = new JsonObject();
                variables.put("USER_ID", userId);
                if (payload.containsKey("email")) {
                    variables.put("USER_EMAIL", payload.getString("email"));
                }
                if (payload.containsKey("role")) {
                    variables.put("ROLE_NAME", payload.getString("role"));
                }

                String toEmail = payload.getString("email", "dev-notification@astc.local");
                String toName = payload.getString("username", "");

                JsonObject msg = new JsonObject();
                msg.put("notificationCode", templateCode);
                msg.put("type", "EMAIL");
                msg.put("userId", userId);
                msg.put("toEmail", toEmail);
                msg.put("toName", toName);
                msg.put("variables", variables);

                LOG.infof("📬 [Outbox] Dispatching payload to RabbitMQ channel 'notification-events': %s", msg.encode());
                notificationEmitter.send(msg.encode());
                LOG.infof("✅ [Outbox] Successfully published outbox event %s (type=%s) as %s to=%s",
                        event.getId(), event.getEventType(), templateCode, toEmail);

                successfulIds.add(event.getId());
            } catch (Exception e) {
                LOG.errorf(e, "❌ [Outbox] Failed to process outbox event %s (type=%s)",
                        event.getId(), event.getEventType());
                markFailedAttempt(event);
            }
        }

        if (!successfulIds.isEmpty()) {
            repository.markPublishedBatch(successfulIds);
        }
    }

    private void markFailedAttempt(OutboxEventEntity managed) {
        int attempts = managed.getAttempts() == null ? 0 : managed.getAttempts();
        managed.setAttempts(attempts + 1);
        if (attempts + 1 >= maxAttempts) {
            LOG.errorf("💀 [Outbox] Event %s exceeded max attempts (%d), marking as FAILED",
                    managed.getId(), maxAttempts);
            managed.setProcessedAt(LocalDateTime.now());
            managed.setStatus("FAILED");
            managed.setLastError("Exceeded max attempts");
        } else {
            managed.setStatus("PENDING");
            managed.setLastError("Processing error, retry pending");
        }
        // Hibernate will auto-update this managed entity at flush time
    }

    private String mapEventType(String eventType) {
        switch (eventType) {
            case "USER_CREATED":
                return "ASB000001";
            case "USER_UPDATED":
                return "ASB000002";
            case "USER_ROLE_CHANGED":
                return "ASB000003";
            case "USER_DELETED":
                return "ASB000004";
            default:
                return null;
        }
    }
}
