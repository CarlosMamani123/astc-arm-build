package com.smms.assistance.domain.model;

import com.smms.assistance.domain.entity.AttendanceEntity;
import com.smms.assistance.domain.entity.JustificationEntity;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private Integer totalAttendances;
    private Integer totalAbsences;
    private Integer pendingJustifications;
}
