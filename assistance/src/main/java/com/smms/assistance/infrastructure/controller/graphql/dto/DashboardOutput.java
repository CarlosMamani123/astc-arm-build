package com.smms.assistance.infrastructure.controller.graphql.dto;

import org.eclipse.microprofile.graphql.Type;
import lombok.*;

@Type
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOutput {

    private Integer totalAttendances;
    private Integer totalAbsences;
    private Integer pendingJustifications;
}