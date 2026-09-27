package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.ListJustificationsPort;
import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.model.Page;
import com.smms.assistance.infrastructure.repository.JustificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListJustificationsUseCase implements ListJustificationsPort {

    @Inject
    JustificationRepository justificationRepository;

    @Override
    public Page<JustificationEntity> execute(
            UUID userId,
            int page,
            int size,
            String status,
            String fromDate,
            String toDate) {
        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        return justificationRepository.findPageByUserNative(
                userId,
                status,
                from,
                to,
                page,
                size);
    }
}