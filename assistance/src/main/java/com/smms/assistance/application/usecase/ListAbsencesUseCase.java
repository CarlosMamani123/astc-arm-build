package com.smms.assistance.application.usecase;

import com.smms.assistance.application.in.ListAbsencesPort;
import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.model.Page;

import com.smms.assistance.infrastructure.repository.AbsenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListAbsencesUseCase implements ListAbsencesPort {

    @Inject
    AbsenceRepository absenceRepository;

    @Override
    public Page<AbsenceEntity> execute(UUID userId, int page, int size, UUID projectId, String fromDate, String toDate,
            String type, Boolean justified) {
        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        return absenceRepository.findPageByUserNative(userId, projectId, from, to, type, justified, page, size);
    }
}
