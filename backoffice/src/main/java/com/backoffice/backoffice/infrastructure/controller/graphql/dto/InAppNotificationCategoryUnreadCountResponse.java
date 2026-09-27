package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import lombok.*;
import org.eclipse.microprofile.graphql.Name;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Name("InAppNotificationCategoryUnreadCount")
public class InAppNotificationCategoryUnreadCountResponse {

    private String category;
    private long count;
}