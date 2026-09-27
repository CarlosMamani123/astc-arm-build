package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.ApproveJustificationPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.entity.JustificationHistoryEntity;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class ApproveJustificationUseCase implements ApproveJustificationPort {

    @Inject JustificationRepository justificationRepository;
    @Inject AbsenceRepository absenceRepository;

    @Override
    @Transactional
    public JustificationEntity execute(UUID justificationId, String comment, UUID reviewedBy) {
        JustificationEntity justification = justificationRepository.findByIdOptional(justificationId)
                .orElseThrow(() -> new IllegalArgumentException("Justification not found"));

        String previousStatus = justification.getStatus();
        if (!"PENDING".equals(previousStatus) && !"SUBMITTED".equals(previousStatus)) {
            throw new IllegalStateException("Can only approve justifications in PENDING or SUBMITTED status. Current: " + previousStatus);
        }

        justification.setStatus("APPROVED");
        justification.setComment(comment);
        justification.setReviewedBy(reviewedBy);
        justification.setReviewedAt(Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime());
        justificationRepository.persist(justification);

        AbsenceEntity absence = absenceRepository.findById(justification.getAbsenceId());
        if (absence != null) {
            absence.setJustified(true);
            absenceRepository.persist(absence);
        }

        recordHistory(justificationId, previousStatus, "APPROVED", comment, reviewedBy);

        System.out.println("[Justification Flow] APPROVED -> ID: " + justificationId + " by: " + reviewedBy);
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
