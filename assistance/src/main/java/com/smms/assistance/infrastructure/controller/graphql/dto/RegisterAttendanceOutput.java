package com.smms.assistance.infrastructure.controller.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.graphql.Type;

@Type
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAttendanceOutput {
    private String id;
    private String userId;
    private String projectId;
    private String date;
    private String checkIn;
    private String checkOut;
    private String status;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
}