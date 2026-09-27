package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.JustifyAbsencePort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.infrastructure.messaging.OutboxEventPublisher;
import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class JustifyAbsenceUseCase implements JustifyAbsencePort {

    private static final Logger LOG = Logger.getLogger(JustifyAbsenceUseCase.class);

    @Inject AbsenceRepository absenceRepository;
    @Inject JustificationRepository justificationRepository;
    @Inject OutboxEventPublisher outboxPublisher;

    @Override
    @Transactional
    public AbsenceEntity execute(UUID userId, UUID absenceId) {
        AbsenceEntity absence = absenceRepository.findById(absenceId);
        if (absence == null) {
            throw new IllegalArgumentException("Absence not found: " + absenceId);
        }
        if (!absence.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: absence does not belong to authenticated user");
        }
        if (Boolean.TRUE.equals(absence.getJustified())) {
            throw new IllegalStateException("Absence already justified");
        }

        absence.setJustified(true);
        absenceRepository.persist(absence);

        LOG.infof("[NOTIFICATION] JustifyAbsenceUseCase executed for absenceId=%s, userId=%s — outbox event DISABLED (commented out)",
                absenceId, userId);

        return absence;
}
}
