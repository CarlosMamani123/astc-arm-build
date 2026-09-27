package com.smms.assistance.infrastructure.client;


import java.util.UUID;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectMemberOutput {
    public UUID id;
    public UUID projectId;
    public UUID userId;
    public UserOutput user;
    public String role;
    public String createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UserOutput getUser() { return user; }
    public void setUser(UserOutput user) { this.user = user; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
