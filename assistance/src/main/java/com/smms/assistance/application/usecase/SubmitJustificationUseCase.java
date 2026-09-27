package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.SubmitJustificationPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.client.ProjectManagerClientWrapper;
import com.smms.assistance.infrastructure.messaging.OutboxEventPublisher;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class SubmitJustificationUseCase implements SubmitJustificationPort {

    @Inject
    JustificationRepository justificationRepository;

    @Inject
    AbsenceRepository absenceRepository;

    @Inject
    ProjectManagerClientWrapper projectManagerClient;

    @Inject
    OutboxEventPublisher outboxPublisher;

    @Inject
    org.eclipse.microprofile.jwt.JsonWebToken jwt;

    @Override
    @Transactional
    public JustificationEntity execute(UUID userId, UUID justificationId) {
        JustificationEntity entity = justificationRepository.findByIdOptional(justificationId)
                .orElseThrow(() -> new IllegalArgumentException("Justification not found: " + justificationId));

        if (!entity.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: justification does not belong to you");
        }

        String currentStatus = entity.getStatus();
        if (!"PENDING".equals(currentStatus) && !"OBSERVATION".equals(currentStatus)) {
            throw new IllegalStateException("Cannot submit justification with status: " + currentStatus);
        }

        String previousStatus = currentStatus;
        entity.setStatus("SUBMITTED");
        entity.setSubmittedAt(java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
        justificationRepository.persist(entity);

        recordHistory(justificationId, previousStatus, "SUBMITTED", null, userId);

        String userEmail = jwt != null ? jwt.getClaim("email") : null;
        String userName = jwt != null ? jwt.getClaim("name") : null;
        if (userEmail == null || userEmail.isBlank()) {
            userEmail = "";
        }
        if (userName == null || userName.isBlank()) {
            userName = userEmail.contains("@") ? userEmail.split("@")[0] : "Colaborador";
        }
        userEmail = userEmail.replace("\"", "\\\"");
        userName = userName.replace("\"", "\\\"");

        String eventType = "OBSERVATION".equals(previousStatus) ? "justification_resubmitted" : "justification_submitted";

        outboxPublisher.publish("Justification", entity.getId(),
                eventType,
                String.format(
                        "{\"justificationId\":\"%s\",\"absenceId\":\"%s\",\"userId\":\"%s\",\"userEmail\":\"%s\",\"userName\":\"%s\"}",
                        entity.getId(),
                        entity.getAbsenceId(),
                        userId,
                        userEmail,
                        userName
                )
        );

        notifyProjectManager(entity);

        System.out.println("[Justification Flow] SUBMITTED -> " + previousStatus + " -> SUBMITTED -> ID: " + justificationId);
        return entity;
    }

    private void notifyProjectManager(JustificationEntity entity) {
        try {
            UUID projectId = absenceRepository.findByIdOptional(entity.getAbsenceId())
                    .map(AbsenceEntity::getProjectId)
                    .orElse(null);
            if (projectId == null) {
                return;
            }

            UUID responsibleId = projectManagerClient.getMyProjects().stream()
                    .filter(p -> projectId.equals(p.id))
                    .map(p -> p.responsibleId)
                    .filter(id -> id != null)
                    .findFirst()
                    .orElse(null);
            if (responsibleId == null) {
                return;
            }

            outboxPublisher.publish("Justification", entity.getId(), "justification_received",
                    String.format(
                            "{\"justificationId\":\"%s\",\"absenceId\":\"%s\",\"userId\":\"%s\"}",
                            entity.getId(),
                            entity.getAbsenceId(),
                            responsibleId
                    ));
        } catch (Exception e) {
            System.out.println("[Justification Flow] Could not queue PM notification for justification "
                    + entity.getId() + ": " + e.getMessage());
        }
    }

    private void recordHistory(UUID justificationId, String previousStatus, String newStatus,
                                String comment, UUID changedBy) {
        JustificationHistoryEntity history = JustificationHistoryEntity.builder()
                .id(UUID.randomUUID())
                .justificationId(justificationId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .comment(comment)
                .changedBy(changedBy)
                .changedAt(java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDateTime())
                .build();
        history.persist();
    }
}
