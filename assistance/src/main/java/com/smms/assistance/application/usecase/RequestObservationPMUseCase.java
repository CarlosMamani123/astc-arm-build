package com.smms.assistance.application.usecase;

import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class RequestObservationPMUseCase {

    @Inject
    JustificationRepository justificationRepository;

    @Transactional
    public JustificationEntity execute(UUID justificationId, String comment, UUID reviewedBy) {
        JustificationEntity justification = justificationRepository.findByIdOptional(justificationId)
                .orElseThrow(() -> new IllegalArgumentException("Justification not found"));

        String previousStatus = justification.getStatus();
        if (!"PENDING".equals(previousStatus) && !"SUBMITTED".equals(previousStatus)) {
            throw new IllegalStateException("Can only request observation for justifications in PENDING or SUBMITTED status. Current: " + previousStatus);
        }

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Observation comment is required");
        }

        justification.setStatus("OBSERVATION");
        justification.setComment(comment);
        justification.setReviewedBy(reviewedBy);
        LocalDateTime utcNow = java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
        justification.setReviewedAt(utcNow);
        justificationRepository.persist(justification);

        recordHistory(justificationId, previousStatus, "OBSERVATION", comment, reviewedBy);

        System.out.println("[Justification Flow] OBSERVATION -> ID: " + justificationId + " by: " + reviewedBy);
        return justification;
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
