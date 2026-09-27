package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.JustificationEntity;
import com.smms.assistance.infrastructure.controller.graphql.dto.SaveJustificationInput;
import java.util.UUID;

public interface SaveJustificationPort {
    JustificationEntity execute(UUID userId, SaveJustificationInput input);
}
