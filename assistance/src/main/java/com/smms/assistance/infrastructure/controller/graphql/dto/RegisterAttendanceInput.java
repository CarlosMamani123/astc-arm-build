package com.smms.assistance.infrastructure.controller.graphql.dto;

import lombok.*;
import org.eclipse.microprofile.graphql.Input;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Input("RegisterAttendanceInput")
public class RegisterAttendanceInput {

    private UUID projectId;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private Boolean replace;
}