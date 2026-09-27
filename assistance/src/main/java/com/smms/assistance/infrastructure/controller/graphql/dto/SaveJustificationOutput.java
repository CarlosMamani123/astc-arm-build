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
public class SaveJustificationOutput {
    private String id;
    private String status;
    private String submittedAt;
}