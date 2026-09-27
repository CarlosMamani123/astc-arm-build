package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import lombok.*;
import org.eclipse.microprofile.graphql.Name;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Name("PaginationResponse")
public class PaginationResponse {

    private int page;
    private int size;
    private long total;
    private List<InAppNotificationResponse> items;
}