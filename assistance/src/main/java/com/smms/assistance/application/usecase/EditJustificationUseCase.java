package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.EditJustificationPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.controller.graphql.dto.SaveJustificationInput;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import com.smms.assistance.infrastructure.storage.S3StorageAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class EditJustificationUseCase implements EditJustificationPort {

    @Inject JustificationRepository justificationRepository;
    @Inject AbsenceRepository absenceRepository;
    @Inject S3StorageAdapter s3StorageAdapter;

    @Override
    @Transactional
    public JustificationEntity execute(UUID userId, UUID justificationId, SaveJustificationInput input) {
        JustificationEntity justification = justificationRepository.findByIdOptional(justificationId)
                .orElseThrow(() -> new IllegalArgumentException("Justification not found: " + justificationId));

        if (!justification.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: justification does not belong to authenticated user");
        }

        String currentStatus = justification.getStatus();
        if (!"PENDING".equals(currentStatus) && !"OBSERVATION".equals(currentStatus)) {
            throw new IllegalStateException("Cannot edit justification with status: " + currentStatus + ". Only PENDING or OBSERVATION can be edited.");
        }

        if (input.getDescription() != null && !input.getDescription().isBlank()) {
            justification.setDescription(input.getDescription());
        }

        if (input.getFileBase64() != null && !input.getFileBase64().isBlank()) {
            try {
                String raw = input.getFileBase64();
                if (raw.contains(",")) {
                    raw = raw.substring(raw.indexOf(",") + 1);
                }
                byte[] decoded = Base64.getDecoder().decode(raw);
                String fileUuid = UUID.randomUUID().toString();
                String folder = "document/private/user/" + userId + "/justifications";
                String filename = fileUuid + ".pdf";
                String key = s3StorageAdapter.upload(folder, filename,
                        new ByteArrayInputStream(decoded), decoded.length, "application/octet-stream");
                justification.setDocumentUrl(key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file to MinIO", e);
            }
        }

        justificationRepository.persist(justification);

        System.out.println("[Justification Flow] EDITED -> ID: " + justificationId);

        return justification;
    }
}
