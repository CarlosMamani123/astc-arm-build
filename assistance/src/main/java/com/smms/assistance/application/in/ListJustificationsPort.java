package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.domain.model.Page;
import java.util.UUID;

public interface ListJustificationsPort {
    Page<JustificationEntity> execute(
            UUID userId,
            int page,
            int size,
            String status,
            String fromDate,
            String toDate);
}