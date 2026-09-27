package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import java.util.UUID;

public class UploadAvatarRequest {
    public UUID userId;
    public String fileName;
}