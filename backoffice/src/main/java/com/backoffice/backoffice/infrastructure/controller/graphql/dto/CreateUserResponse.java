package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

public class CreateUserResponse {

    private String id;
    private String username;
    private String email;

    private String firstName;
    private String lastName;

    private String phone;
    private String roleId;

    private String avatarUrl;

    private String roleCode;
    private String roleName;

    public CreateUserResponse() {
    }

    public CreateUserResponse(String id,
                              String username,
                              String email,
                              String firstName,
                              String lastName,
                              String phone,
                              String roleId,
                              String avatarUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.roleId = roleId;
        this.avatarUrl = avatarUrl;
    }

    public CreateUserResponse(String id,
                              String username,
                              String email,
                              String firstName,
                              String lastName,
                              String phone,
                              String roleId,
                              String avatarUrl,
                              String roleCode,
                              String roleName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.roleId = roleId;
        this.avatarUrl = avatarUrl;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}