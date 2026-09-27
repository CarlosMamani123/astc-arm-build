package com.authservice.authservice.infrastructure.controller.graphql.dto;

import java.util.UUID;

public class AuthUserOutput {

    private UUID id;
    private String email;
    private String passwordHash;
    private UUID roleId;

    public AuthUserOutput(UUID id, String email, String passwordHash, UUID roleId) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UUID getRoleId() {
        return roleId;
    }
}