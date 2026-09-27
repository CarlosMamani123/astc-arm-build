package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.infrastructure.controller.graphql.dto.RegisterAttendanceInput;
import java.util.UUID;

public interface RegisterAttendancePort {
    AttendanceEntity execute(UUID userId, RegisterAttendanceInput input);
}
