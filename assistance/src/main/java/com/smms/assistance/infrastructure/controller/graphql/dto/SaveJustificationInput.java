package com.smms.assistance.infrastructure.controller.graphql.dto;

import lombok.*;
import org.eclipse.microprofile.graphql.Input;
import java.util.UUID;

@Input
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveJustificationInput {

    private UUID absenceId;
    private String description;
    private String documentUrl;
    private String fileBase64;
}