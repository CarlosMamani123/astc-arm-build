package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.GetJustificationStatusPort;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class GetJustificationStatusUseCase implements GetJustificationStatusPort {

    @Inject JustificationRepository justificationRepository;

    @Override
    public JustificationEntity execute(UUID userId, UUID justificationId) {
        JustificationEntity entity = justificationRepository.findById(justificationId);
        if (entity == null) {
            throw new IllegalArgumentException("Justification not found: " + justificationId);
        }
        if (!entity.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: justification does not belong to authenticated user");
        }
        return entity;
    }
}
