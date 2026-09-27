package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.RejectJustificationPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class RejectJustificationUseCase implements RejectJustificationPort {

    @Inject JustificationRepository justificationRepository;
    @Inject AbsenceRepository absenceRepository;

    @Override
    @Transactional
    public JustificationEntity execute(UUID justificationId, String comment, UUID reviewedBy) {
        JustificationEntity justification = justificationRepository.findByIdOptional(justificationId)
                .orElseThrow(() -> new IllegalArgumentException("Justification not found"));

        String previousStatus = justification.getStatus();
        if (!"PENDING".equals(previousStatus) && !"SUBMITTED".equals(previousStatus)) {
            throw new IllegalStateException("Can only reject justifications in PENDING or SUBMITTED status. Current: " + previousStatus);
        }

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Review comment is required for rejection");
        }

        justification.setStatus("REJECTED");
        justification.setComment(comment);
        justification.setReviewedBy(reviewedBy);
        justification.setReviewedAt(Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime());
        justificationRepository.persist(justification);

        AbsenceEntity absence = absenceRepository.findById(justification.getAbsenceId());
        if (absence != null) {
            absence.setJustified(false);
            absenceRepository.persist(absence);
        }

        recordHistory(justificationId, previousStatus, "REJECTED", comment, reviewedBy);

        System.out.println("[Justification Flow] REJECTED -> ID: " + justificationId + " by: " + reviewedBy);
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
                .changedAt(Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime())
                .build();
        history.persist();
    }
}
