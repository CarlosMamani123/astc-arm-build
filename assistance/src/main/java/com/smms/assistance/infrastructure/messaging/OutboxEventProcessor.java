package com.smms.assistance.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.OutboxEventEntity;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.AttendanceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import com.smms.assistance.infrastructure.repository.OutboxRepository;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class OutboxEventProcessor {

    private static final Logger LOG = Logger.getLogger(OutboxEventProcessor.class);

    private static final java.util.Map<String, String> STATUS_TRANSLATIONS = new java.util.HashMap<>();
    static {
        STATUS_TRANSLATIONS.put("ON_TIME", "A tiempo");
        STATUS_TRANSLATIONS.put("ON_TIME_CHECKED_OUT", "A tiempo");
        STATUS_TRANSLATIONS.put("LATE", "Tarde");
        STATUS_TRANSLATIONS.put("LATE_CHECKED_OUT", "Tarde");
        STATUS_TRANSLATIONS.put("EARLY", "Fuera de horario");
        STATUS_TRANSLATIONS.put("OUTSIDE_SCHEDULE", "Fuera de horario");
        STATUS_TRANSLATIONS.put("EARLY_DEPARTURE", "Salida temprana");
        STATUS_TRANSLATIONS.put("CHECKED_IN", "Registrado");
        STATUS_TRANSLATIONS.put("PRESENTE", "Presente");
        STATUS_TRANSLATIONS.put("FALTA", "Falta");
        STATUS_TRANSLATIONS.put("UNJUSTIFIED", "No justificada");
        STATUS_TRANSLATIONS.put("PENDING", "Borrador");
        STATUS_TRANSLATIONS.put("SUBMITTED", "Enviado");
        STATUS_TRANSLATIONS.put("OBSERVATION", "Observado");
        STATUS_TRANSLATIONS.put("APPROVED", "Aprobado");
        STATUS_TRANSLATIONS.put("REJECTED", "Rechazado");
    }

    private static String translateStatus(String rawStatus) {
        if (rawStatus == null) return "";
        String translated = STATUS_TRANSLATIONS.get(rawStatus.toUpperCase());
        return translated != null ? translated : rawStatus;
    }

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    AttendanceRepository attendanceRepository;

    @Inject
    AbsenceRepository absenceRepository;

    @Inject
    JustificationRepository justificationRepository;

    @Inject
    @Channel("notification-events")
    Emitter<String> notificationEmitter;

    @Inject
    com.smms.assistance.infrastructure.client.AuthClient authClient;

    @Inject
    com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper projectManagerClient;

    @ConfigProperty(name = "event.notification.template.attendance_registered", defaultValue = "AST00001")
    String attendanceTemplateCode;

    @ConfigProperty(name = "event.notification.template.attendance_early", defaultValue = "AST00002")
    String attendanceEarlyTemplateCode;

    @ConfigProperty(name = "event.notification.template.attendance_late", defaultValue = "AST00003")
    String attendanceLateTemplateCode;

    @ConfigProperty(name = "event.notification.template.attendance_outside_hours", defaultValue = "AST00004")
    String attendanceOutsideHoursTemplateCode;

    @ConfigProperty(name = "event.notification.template.absence_late", defaultValue = "AST00005")
    String absenceLateTemplateCode;

    @ConfigProperty(name = "event.notification.template.absence_late_pm", defaultValue = "AST00006")
    String absenceLatePmTemplateCode;

    @ConfigProperty(name = "event.notification.template.absence_no_record", defaultValue = "AST00007")
    String absenceNoRecordTemplateCode;

    @ConfigProperty(name = "event.notification.template.absence_no_record_pm", defaultValue = "AST00008")
    String absenceNoRecordPmTemplateCode;

    @ConfigProperty(name = "event.notification.template.checkout_registered", defaultValue = "AST00009")
    String checkoutTemplateCode;

    @ConfigProperty(name = "event.notification.template.checkout_early", defaultValue = "AST00010")
    String checkoutEarlyTemplateCode;

    @ConfigProperty(name = "event.notification.template.absence_early_checkout", defaultValue = "AST00011")
    String absenceEarlyCheckoutTemplateCode;

    @ConfigProperty(name = "event.notification.template.justification_submitted", defaultValue = "AST00012")
    String justificationSubmittedTemplateCode;

    @ConfigProperty(name = "event.notification.template.justification_received", defaultValue = "AST00013")
    String justificationReceivedTemplateCode;

    @ConfigProperty(name = "event.notification.template.justification_resubmitted", defaultValue = "AST00014")
    String justificationResubmittedTemplateCode;

    @ConfigProperty(name = "event.notification.default.email", defaultValue = "dev-notification@astc.local")
    String defaultEmail;

    @ConfigProperty(name = "event.outbox.batch-size", defaultValue = "50")
    int batchSize;

    @ConfigProperty(name = "event.outbox.max-attempts", defaultValue = "10")
    int maxAttempts;

    @Inject
    Vertx vertx;

    void init(@Observes StartupEvent ev) {
        vertx.setPeriodic(5000, id -> vertx.executeBlocking(
            promise -> {
                try {
                    processEvents();
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
        List<OutboxEventEntity> events;
        try {
            events = io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().call(() -> 
                outboxRepository.findUnpublished(batchSize)
            );
        } catch (Exception e) {
            LOG.error("Failed to fetch outbox events", e);
            return;
        }
        if (events.isEmpty()) {
            return;
        }

        LOG.infof("📬 [Outbox] Found %d unpublished outbox events to process", events.size());

        java.util.List<UUID> successfulIds = new java.util.ArrayList<>();

        // Extract IDs for batch preloading
        java.util.Set<UUID> attendanceIds = new java.util.HashSet<>();
        java.util.Set<UUID> absenceIds = new java.util.HashSet<>();
        java.util.Set<UUID> justificationIds = new java.util.HashSet<>();

        for (OutboxEventEntity event : events) {
            try {
                JsonObject payload = new JsonObject(event.getPayload());
                String attId = payload.getString("attendanceId");
                if (attId != null) attendanceIds.add(UUID.fromString(attId));
                String absId = payload.getString("absenceId");
                if (absId != null) absenceIds.add(UUID.fromString(absId));
                String justId = payload.getString("justificationId");
                if (justId != null) justificationIds.add(UUID.fromString(justId));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Batch load entities
        java.util.Map<UUID, AttendanceEntity> attendanceMap = new java.util.HashMap<>();
        if (!attendanceIds.isEmpty()) {
            try {
                List<AttendanceEntity> list = io.quarkus.narayana.jta.QuarkusTransaction.requiringNew()
                        .call(() -> attendanceRepository.find("id in ?1", attendanceIds).list());
                for (AttendanceEntity a : list) attendanceMap.put(a.getId(), a);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to batch load attendance entities for %d ids", attendanceIds.size());
            }
        }

        java.util.Map<UUID, AbsenceEntity> absenceMap = new java.util.HashMap<>();
        if (!absenceIds.isEmpty()) {
            try {
                List<AbsenceEntity> list = io.quarkus.narayana.jta.QuarkusTransaction.requiringNew()
                        .call(() -> absenceRepository.find("id in ?1", absenceIds).list());
                for (AbsenceEntity a : list) absenceMap.put(a.getId(), a);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to batch load absence entities for %d ids", absenceIds.size());
            }
        }

        java.util.Map<UUID, JustificationEntity> justificationMap = new java.util.HashMap<>();
        if (!justificationIds.isEmpty()) {
            try {
                List<JustificationEntity> list = io.quarkus.narayana.jta.QuarkusTransaction.requiringNew()
                        .call(() -> justificationRepository.find("id in ?1", justificationIds).list());
                for (JustificationEntity j : list) justificationMap.put(j.getId(), j);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to batch load justification entities for %d ids", justificationIds.size());
            }
        }

        for (OutboxEventEntity event : events) {
            try {
                io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
                    LOG.infof("📬 [Outbox] Processing event ID=%s, type=%s, payload=%s",
                            event.getId(), event.getEventType(), event.getPayload());
                    JsonObject notificationMsg = mapToNotification(event, attendanceMap, absenceMap, justificationMap);
                    if (notificationMsg != null) {
                        notificationEmitter.send(notificationMsg.encode());
                        LOG.infof("✅ [Outbox] Successfully published outbox event %s (type=%s) as %s to=%s",
                                event.getId(), event.getEventType(),
                                notificationMsg.getString("notificationCode"),
                                notificationMsg.getString("toEmail"));
                    } else {
                        LOG.warnf("⚠ [Outbox] Event %s mapped to a null notification payload, marking as processed", event.getId());
                    }
                    successfulIds.add(event.getId());
                });
            } catch (Exception e) {
                LOG.errorf(e, "❌ [Outbox] Failed to process outbox event %s (type=%s)",
                        event.getId(), event.getEventType());
                try {
                    io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> markFailedAttempt(event));
                } catch (Exception ex) {
                    LOG.errorf(ex, "❌ [Outbox] Failed to record failed attempt for event %s", event.getId());
                }
            }
        }

        if (!successfulIds.isEmpty()) {
            try {
                io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
                    outboxRepository.getEntityManager().createNativeQuery(
                            "UPDATE outbox_events SET published = true, published_at = now(), status = 'PUBLISHED', last_error = null WHERE id = ANY(:ids)")
                            .setParameter("ids", successfulIds.toArray(new UUID[0]))
                            .executeUpdate();
                });
                LOG.infof("✅ [Outbox] Bulk updated %d events as PUBLISHED", successfulIds.size());
            } catch (Exception e) {
                LOG.error("Failed to perform bulk update for published outbox events, executing fallback", e);
                for (UUID id : successfulIds) {
                    try {
                        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
                            outboxRepository.getEntityManager().createNativeQuery(
                                    "UPDATE outbox_events SET published = true, published_at = now(), status = 'PUBLISHED', last_error = null WHERE id = :id")
                                    .setParameter("id", id)
                                    .executeUpdate();
                        });
                    } catch (Exception ex) {
                        LOG.errorf(ex, "Failed fallback update for outbox event %s", id);
                    }
                }
            }
        }
    }

    private void markPublished(OutboxEventEntity event) {
        try {
            outboxRepository.update("published = true, publishedAt = ?1, status = 'PUBLISHED', lastError = null WHERE id = ?2",
                    LocalDateTime.now(), event.getId());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to mark outbox event %s as PUBLISHED", event.getId());
        }
    }

    private void markFailedAttempt(OutboxEventEntity event) {
        try {
            int attempts = event.getAttempts() == null ? 0 : event.getAttempts();
            int newAttempts = attempts + 1;
            if (newAttempts >= maxAttempts) {
                LOG.errorf("💀 [Outbox] Event %s exceeded max attempts (%d), marking as FAILED",
                        event.getId(), maxAttempts);
                outboxRepository.update("published = true, publishedAt = ?1, status = 'FAILED', lastError = 'Exceeded max attempts', attempts = ?2 WHERE id = ?3",
                        LocalDateTime.now(), newAttempts, event.getId());
            } else {
                outboxRepository.update("status = 'PENDING', lastError = 'Processing error, retry pending', attempts = ?1 WHERE id = ?2",
                        newAttempts, event.getId());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to record failed attempt for outbox event %s", event.getId());
        }
    }

    private void enrichUserDetails(JsonObject payload, String userId) {
        String toEmail = payload.getString("userEmail", "");
        if (toEmail == null || toEmail.isBlank()) {
            if (userId != null && !userId.isBlank()) {
                try {
                    com.smms.assistance.infrastructure.client.UserOutput user = authClient.getUser(userId);
                    if (user != null) {
                        payload.put("userEmail", user.email);
                        payload.put("userName", user.username);
                    }
                } catch (Exception e) {
                    LOG.warnf("Could not fetch user details from auth-service for userId=%s: %s", userId, e.getMessage());
                }
            }
        }
    }

    private JsonObject mapToNotification(OutboxEventEntity event,
                                        java.util.Map<UUID, AttendanceEntity> attendanceMap,
                                        java.util.Map<UUID, AbsenceEntity> absenceMap,
                                        java.util.Map<UUID, JustificationEntity> justificationMap) {
        JsonObject payload = new JsonObject(event.getPayload());
        String userId = payload.getString("userId");

        if (userId == null || userId.isBlank()) {
            LOG.warnf("Skipping outbox event %s: missing userId in payload", event.getId());
            return null;
        }

        enrichUserDetails(payload, userId);

        switch (event.getEventType()) {
            case "attendance_registered":
                return buildMessage(event, payload, userId, attendanceTemplateCode, attendanceMap);
            case "attendance_early":
                return buildMessage(event, payload, userId, attendanceEarlyTemplateCode, attendanceMap);
            case "attendance_late":
                return buildMessage(event, payload, userId, attendanceLateTemplateCode, attendanceMap);
            case "attendance_outside_hours":
                return buildMessage(event, payload, userId, attendanceOutsideHoursTemplateCode, attendanceMap);
            case "absence_late":
                return buildMessage(event, payload, userId, absenceLateTemplateCode, attendanceMap);
            case "absence_late_pm":
                return buildMessage(event, payload, userId, absenceLatePmTemplateCode, attendanceMap);
            case "absence_no_record":
                return buildMessage(event, payload, userId, absenceNoRecordTemplateCode, attendanceMap);
            case "absence_no_record_pm":
                return buildMessage(event, payload, userId, absenceNoRecordPmTemplateCode, attendanceMap);
            case "checkout_registered":
                return buildMessage(event, payload, userId, checkoutTemplateCode, attendanceMap);
            case "checkout_early":
                return buildMessage(event, payload, userId, checkoutEarlyTemplateCode, attendanceMap);
            case "absence_early_checkout":
                return buildMessage(event, payload, userId, absenceEarlyCheckoutTemplateCode, attendanceMap);
            case "SendNotificationByQueue":
                return buildJustificationMessage(event, payload, userId, justificationSubmittedTemplateCode, absenceMap, justificationMap);
            case "justification_submitted":
                return buildJustificationMessage(event, payload, userId, justificationSubmittedTemplateCode, absenceMap, justificationMap);
            case "justification_received":
                return buildJustificationMessage(event, payload, userId, justificationReceivedTemplateCode, absenceMap, justificationMap);
            case "justification_resubmitted":
                return buildJustificationMessage(event, payload, userId, justificationResubmittedTemplateCode, absenceMap, justificationMap);
            default:
                LOG.warnf("Unknown event type '%s' for outbox event %s, skipping",
                        event.getEventType(), event.getId());
                return null;
        }
    }

    private JsonObject buildMessage(OutboxEventEntity event, JsonObject payload, String userId, String templateCode,
                                    java.util.Map<UUID, AttendanceEntity> attendanceMap) {
        JsonObject variables = new JsonObject();
        variables.put("USER_ID", userId);

        String attendanceId = payload.getString("attendanceId");
        if (attendanceId != null) {
            variables.put("ATTENDANCE_ID", attendanceId);
            AttendanceEntity attendance = attendanceMap.get(UUID.fromString(attendanceId));
            if (attendance != null) {
                if (attendance.getProjectId() != null) {
                    variables.put("PROJECT_ID", attendance.getProjectId().toString());
                    try {
                        var projects = projectManagerClient.getMyProjects();
                        for (var p : projects) {
                            if (attendance.getProjectId().equals(p.getId())) {
                                variables.put("PROJECT_NAME", p.getName());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        LOG.warnf("Could not resolve project name for %s", attendance.getProjectId());
                    }
                }
                if (attendance.getCheckIn() != null) {
                    variables.put("CHECKIN_TIME", attendance.getCheckIn().toString());
                }
                if (attendance.getDate() != null) {
                    variables.put("ATTENDANCE_DATE", attendance.getDate().toString());
                }
                variables.put("STATUS", translateStatus(attendance.getStatus()));
            }
        }

        String toName = payload.getString("userName", "");
        String toEmail = payload.getString("userEmail", "");
        if (toEmail.isBlank()) {
            toEmail = variables.getString("USER_EMAIL", defaultEmail);
        }

        return formatEmailMessage(templateCode, userId, toEmail, toName, variables);
    }

    private JsonObject buildJustificationMessage(OutboxEventEntity event, JsonObject payload, String userId, String templateCode,
                                                 java.util.Map<UUID, AbsenceEntity> absenceMap,
                                                 java.util.Map<UUID, JustificationEntity> justificationMap) {
        JsonObject variables = new JsonObject();
        variables.put("USER_ID", userId);

        String justificationId = payload.getString("justificationId");
        String absenceId = payload.getString("absenceId");
        if (justificationId != null) {
            variables.put("JUSTIFICATION_ID", justificationId);
        }
        if (absenceId != null) {
            variables.put("ABSENCE_ID", absenceId);
            AbsenceEntity absence = absenceMap.get(UUID.fromString(absenceId));
            if (absence != null) {
                if (absence.getProjectId() != null) {
                    variables.put("PROJECT_ID", absence.getProjectId().toString());
                }
                if (absence.getDate() != null) {
                    variables.put("ABSENCE_DATE", absence.getDate().toString());
                }
            }
        }

        if (justificationId != null) {
            JustificationEntity justification = justificationMap.get(UUID.fromString(justificationId));
            if (justification != null) {
                variables.put("STATUS", translateStatus(justification.getStatus()));
            }
        }

        String toName = payload.getString("userName", "");
        String toEmail = payload.getString("userEmail", "");
        if (toEmail.isBlank()) {
            toEmail = defaultEmail;
        }

        return formatEmailMessage(templateCode, userId, toEmail, toName, variables);
    }


    private JsonObject formatEmailMessage(String notificationCode, String userId, String toEmail, String toName, JsonObject variables) {
        JsonObject msg = new JsonObject();
        msg.put("notificationCode", notificationCode);
        msg.put("type", "EMAIL");
        msg.put("userId", userId);
        msg.put("toEmail", toEmail);
        msg.put("toName", toName);
        msg.put("variables", variables);
        return msg;
    }
}
