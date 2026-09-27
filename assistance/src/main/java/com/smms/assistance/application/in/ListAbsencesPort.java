package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.AbsenceEntity;
import com.smms.assistance.domain.model.Page;

import java.util.UUID;

public interface ListAbsencesPort {
    Page<AbsenceEntity> execute(UUID userId, int page, int size, UUID projectId, String fromDate, String toDate, String type, Boolean justified);
}
