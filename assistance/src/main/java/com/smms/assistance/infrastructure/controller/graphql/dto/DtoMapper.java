package com.smms.assistance.infrastructure.controller.graphql.dto;

import com.smms.assistance.domain.entity.*;
import com.smms.assistance.domain.model.*;

import com.smms.assistance.shared.util.TimezoneService;

import java.time.LocalDateTime;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    /** Convert UTC-stored LocalDateTime to target timezone string for display */
    public static String utcToZone(LocalDateTime utc, String targetZoneId) {
        if (utc == null || targetZoneId == null) return null;
        return TimezoneService.convertUtcToZone(utc, targetZoneId);
    }

    /** @deprecated Use utcToZone(utc, zoneId) instead */
    @Deprecated
    public static String utcToLima(LocalDateTime utc) {
        return utcToZone(utc, "America/Lima");
    }

    public static AttendanceOutput toAttendanceOutput(AttendanceEntity e) {
        return AttendanceOutput.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .projectId(e.getProjectId())
                .date(e.getDate() != null ? e.getDate().toString() : null)
                .checkIn(e.getCheckIn() != null ? e.getCheckIn().toString() : null)
                .checkOut(e.getCheckOut() != null ? e.getCheckOut().toString() : null)
                .status(e.getStatus())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .photoUrl(e.getPhotoUrl())
                .build();
    }

    public static AbsenceOutput toAbsenceOutput(AbsenceEntity e) {
        return AbsenceOutput.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .projectId(e.getProjectId())
                .date(e.getDate() != null ? e.getDate().toString() : null)
                .type(e.getType())
                .justified(e.getJustified())
                .build();
    }

    public static JustificationOutput toJustificationOutput(JustificationEntity e) {
        return JustificationOutput.builder()
                .id(e.getId())
                .absenceId(e.getAbsenceId())
                .userId(e.getUserId())
                .description(e.getDescription())
                .documentUrl(e.getDocumentUrl())
                .status(e.getStatus())
                .comment(e.getComment())
                .submittedAt(e.getSubmittedAt() != null ? utcToZone(e.getSubmittedAt(), TimezoneService.defaultTimezone()) : null)
                .reviewedAt(e.getReviewedAt() != null ? utcToZone(e.getReviewedAt(), TimezoneService.defaultTimezone()) : null)
                .reviewedBy(e.getReviewedBy())
                .build();
    }

    public static JustificationOutput toJustificationOutput(JustificationEntity e, String documentUrl) {
        return JustificationOutput.builder()
                .id(e.getId())
                .absenceId(e.getAbsenceId())
                .userId(e.getUserId())
                .description(e.getDescription())
                .documentUrl(documentUrl)
                .status(e.getStatus())
                .comment(e.getComment())
                .submittedAt(e.getSubmittedAt() != null ? utcToZone(e.getSubmittedAt(), TimezoneService.defaultTimezone()) : null)
                .reviewedAt(e.getReviewedAt() != null ? utcToZone(e.getReviewedAt(), TimezoneService.defaultTimezone()) : null)
                .reviewedBy(e.getReviewedBy())
                .build();
    }

    public static JustificationStatusOutput toJustificationStatusOutput(JustificationEntity e) {
        return JustificationStatusOutput.builder()
                .justificationId(e.getId())
                .status(e.getStatus())
                .build();
    }

    public static DashboardOutput toDashboardOutput(DashboardSummary s) {
        return DashboardOutput.builder()
                .totalAttendances(s.getTotalAttendances())
                .totalAbsences(s.getTotalAbsences())
                .pendingJustifications(s.getPendingJustifications())
                .build();
    }

    public static <T> AttendancePageOutput toAttendancePage(Page<AttendanceEntity> p) {
        return AttendancePageOutput.builder()
                .items(p.getItems().stream().map(DtoMapper::toAttendanceOutput).toList())
                .page(p.getPage())
                .size(p.getSize())
                .total(p.getTotalItems())
                .build();
    }

    public static JustificationPageOutput toJustificationPage(Page<JustificationEntity> p) {
        return JustificationPageOutput.builder()
                .items(p.getItems().stream().map(DtoMapper::toJustificationOutput).toList())
                .page(p.getPage())
                .size(p.getSize())
                .total(p.getTotalItems())
                .build();
    }

    public static AbsencePageOutput toAbsencePage(Page<AbsenceEntity> p) {
        return AbsencePageOutput.builder()
                .items(p.getItems().stream().map(DtoMapper::toAbsenceOutput).toList())
                .page(p.getPage())
                .size(p.getSize())
                .total(p.getTotalItems())
                .build();
    }

    public static RegisterAttendanceOutput toRegisterAttendanceOutput(AttendanceEntity entity) {
        if (entity == null)
            return null;

        return RegisterAttendanceOutput.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .userId(entity.getUserId() != null ? entity.getUserId().toString() : null)
                .projectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null)
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .checkIn(entity.getCheckIn() != null ? entity.getCheckIn().toString() : null)
                .checkOut(entity.getCheckOut() != null ? entity.getCheckOut().toString() : null)
                .status(entity.getStatus())
                .photoUrl(entity.getPhotoUrl())
                .latitude(entity.getLatitude() != null ? entity.getLatitude().doubleValue() : null)
                .longitude(entity.getLongitude() != null ? entity.getLongitude().doubleValue() : null)
                .build();
    }

    public static SaveJustificationOutput toSaveJustificationOutput(JustificationEntity entity) {
        if (entity == null)
            return null;

        return SaveJustificationOutput.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt() != null ? utcToZone(entity.getSubmittedAt(), TimezoneService.defaultTimezone()) : null)
                .build();
    }
}
