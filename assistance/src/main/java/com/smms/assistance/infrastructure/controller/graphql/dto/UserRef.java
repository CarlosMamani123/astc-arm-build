package com.smms.assistance.infrastructure.controller.graphql.dto;

import java.util.UUID;

public class UserRef {

    private UUID id;

    public UserRef() {
    }

    public UserRef(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}