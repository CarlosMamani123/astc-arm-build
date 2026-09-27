package com.smms.assistance.infrastructure.client;

import java.util.UUID;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectOutput {
    public UUID id;
    public String name;
    public Double latitude;
    public Double longitude;
    public Double radius;
    public String workStartTime;
    public String workEndTime;
    public Integer graceMinutes;
    public String timezone;
    public Integer vacationEligibilityDays;
    public java.util.List<String> holidays;
    public String status;
    public String startDate;
    public String endDate;
    public java.math.BigDecimal budget;
    public String currency;
    public String shiftType;
    public UUID responsibleId;
    public String createdAt;
    public String absenceCutoffTime;
    public String description;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public String getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(String workStartTime) {
        this.workStartTime = workStartTime;
    }

    public String getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(String workEndTime) {
        this.workEndTime = workEndTime;
    }

    public Integer getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(Integer graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getVacationEligibilityDays() {
        return vacationEligibilityDays;
    }

    public void setVacationEligibilityDays(Integer vacationEligibilityDays) {
        this.vacationEligibilityDays = vacationEligibilityDays;
    }

    public java.util.List<String> getHolidays() { return holidays; }
    public void setHolidays(java.util.List<String> holidays) { this.holidays = holidays; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    
    public java.math.BigDecimal getBudget() { return budget; }
    public void setBudget(java.math.BigDecimal budget) { this.budget = budget; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    
    public UUID getResponsibleId() { return responsibleId; }
    public void setResponsibleId(UUID responsibleId) { this.responsibleId = responsibleId; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getAbsenceCutoffTime() { return absenceCutoffTime; }
    public void setAbsenceCutoffTime(String absenceCutoffTime) { this.absenceCutoffTime = absenceCutoffTime; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
