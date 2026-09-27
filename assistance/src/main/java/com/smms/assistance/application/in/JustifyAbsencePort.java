package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.AbsenceEntity;
import java.util.UUID;

public interface JustifyAbsencePort {
    AbsenceEntity execute(UUID userId, UUID absenceId);
}
