package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import lombok.*;
import org.eclipse.microprofile.graphql.Name;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Name("InAppNotification")
public class InAppNotificationResponse {

    private UUID id;
    private UUID userId;
    private String notificationCode;
    private String title;
    private String body;
    private String icon;
    private String color;
    private String actionLink;
    private boolean read;
    private Instant createdAt;

    public static InAppNotificationResponse fromEntity(com.backoffice.backoffice.domain.notification.InAppNotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return InAppNotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .notificationCode(entity.getNotificationCode())
                .title(entity.getTitle())
                .body(entity.getBody())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .actionLink(entity.getActionLink())
                .read(entity.getRead() != null && entity.getRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}