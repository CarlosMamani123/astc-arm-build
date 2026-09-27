package com.authservice.authservice.infrastructure.controller.graphql.dto;

import org.eclipse.microprofile.graphql.Type;

import java.util.UUID;

@Type("Role")
public class RoleOutput {

    private UUID id;
    private String code;
    private String name;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
