package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.SaveJustificationPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.controller.graphql.dto.SaveJustificationInput;
import com.smms.assistance.infrastructure.messaging.OutboxEventPublisher;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import com.smms.assistance.infrastructure.storage.S3StorageAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class SaveJustificationUseCase implements SaveJustificationPort {

    private static final Logger LOG = Logger.getLogger(SaveJustificationUseCase.class);

    @Inject JustificationRepository justificationRepository;
    @Inject AbsenceRepository absenceRepository;
    @Inject OutboxEventPublisher outboxPublisher;
    @Inject S3StorageAdapter s3StorageAdapter;
    @Inject org.eclipse.microprofile.jwt.JsonWebToken jwt;

    @Override
    @Transactional
    public JustificationEntity execute(UUID userId, SaveJustificationInput input) {
        AbsenceEntity absence = absenceRepository.findById(input.getAbsenceId());
        if (absence == null) {
            throw new IllegalArgumentException("Absence not found: " + input.getAbsenceId());
        }
        if (!absence.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: absence does not belong to authenticated user");
        }

        justificationRepository.findByAbsenceId(input.getAbsenceId()).ifPresent(j -> {
            throw new IllegalStateException("A justification already exists for absence: " + input.getAbsenceId());
        });

        String documentUrl = input.getDocumentUrl();

        if (input.getFileBase64() != null && !input.getFileBase64().isBlank()) {
            try {
                String rawBase64 = input.getFileBase64();
                String raw = rawBase64;
                if (raw.contains(",")) {
                    raw = raw.substring(raw.indexOf(",") + 1);
                }
                byte[] decoded = Base64.getDecoder().decode(raw);
                String mimeType = "application/octet-stream";
                String fileExt = ".pdf";
                if (rawBase64.startsWith("data:")) {
                    int mimeEnd = rawBase64.indexOf(";");
                    if (mimeEnd > 5) {
                        mimeType = rawBase64.substring(5, mimeEnd);
                    }
                    if (mimeType.startsWith("image/")) {
                        String ext = mimeType.substring(6);
                        if ("jpeg".equals(ext)) ext = "jpg";
                        fileExt = "." + ext;
                    } else if (mimeType.equals("application/pdf")) {
                        fileExt = ".pdf";
                    }
                }
                String fileUuid = UUID.randomUUID().toString();
                String folder = "document/private/user/" + userId + "/justifications";
                String filename = fileUuid + fileExt;
                String key = s3StorageAdapter.upload(folder, filename,
                        new ByteArrayInputStream(decoded), decoded.length, mimeType);
                documentUrl = key;
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file to MinIO", e);
            }
        }

        UUID justificationId = UUID.randomUUID();
        JustificationEntity entity = JustificationEntity.builder()
                .id(justificationId)
                .absenceId(input.getAbsenceId())
                .userId(userId)
                .description(input.getDescription())
                .documentUrl(documentUrl)
                .status("PENDING")
                .submittedAt(null)
                .build();

        justificationRepository.persist(entity);

        recordHistory(justificationId, null, "PENDING", "Justification created", userId);

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

        LOG.infof("[OUTBOX] Publishing justification event: justificationId=%s, absenceId=%s, userId=%s",
                entity.getId(), input.getAbsenceId(), userId);
        outboxPublisher.publish("Justification", entity.getId(),
                "SendNotificationByQueue",
                String.format(
                        "{\"justificationId\":\"%s\",\"absenceId\":\"%s\",\"userId\":\"%s\",\"userEmail\":\"%s\",\"userName\":\"%s\"}",
                        entity.getId(),
                        input.getAbsenceId(),
                        userId,
                        userEmail,
                        userName
                )
        );

        LOG.infof("[Justification Flow] CREATED -> ID: %s, Status: PENDING", justificationId);
        return entity;
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
