package com.smms.assistance.application.in;

import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.domain.model.Page;
import java.util.UUID;

public interface ListAttendancePort {
    Page<AttendanceEntity> execute(
            UUID userId,
            int page,
            int size,
            UUID projectId,
            String fromDate,
            String toDate,
            String status);
}