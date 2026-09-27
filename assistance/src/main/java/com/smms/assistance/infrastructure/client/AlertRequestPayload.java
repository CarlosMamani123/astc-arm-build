package com.smms.assistance.infrastructure.client;

import java.util.UUID;

public class AlertRequestPayload {
    public UUID attendanceId;
    public UUID userId;
    public UUID projectId;
    public String date;
    public String checkIn;
    public Double latitude;
    public Double longitude;
    public String photoUrl;
    public Double projectLatitude;
    public Double projectLongitude;
    public Double projectRadius;
    public String workStartTime;
    public String workEndTime;
    public Integer graceMinutes;

    public AlertRequestPayload() {}

    public AlertRequestPayload(UUID attendanceId, UUID userId, UUID projectId, String date, String checkIn,
                               Double latitude, Double longitude, String photoUrl, Double projectLatitude,
                               Double projectLongitude, Double projectRadius, String workStartTime,
                               String workEndTime, Integer graceMinutes) {
        this.attendanceId = attendanceId;
        this.userId = userId;
        this.projectId = projectId;
        this.date = date;
        this.checkIn = checkIn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = photoUrl;
        this.projectLatitude = projectLatitude;
        this.projectLongitude = projectLongitude;
        this.projectRadius = projectRadius;
        this.workStartTime = workStartTime;
        this.workEndTime = workEndTime;
        this.graceMinutes = graceMinutes;
    }
}
