package com.smms.assistance.infrastructure.controller.graphql.dto;

import lombok.*;
import java.util.UUID;
import org.eclipse.microprofile.graphql.Type;

@Type
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JustificationStatusOutput {
    private UUID justificationId;
    private String status;
}
