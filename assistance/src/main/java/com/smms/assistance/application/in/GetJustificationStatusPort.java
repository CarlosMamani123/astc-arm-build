package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.JustificationEntity;
import java.util.UUID;

public interface GetJustificationStatusPort {
    JustificationEntity execute(UUID userId, UUID justificationId);
}
