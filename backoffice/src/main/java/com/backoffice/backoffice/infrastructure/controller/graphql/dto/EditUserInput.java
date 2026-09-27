package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import org.eclipse.microprofile.graphql.Input;

@Input
public class EditUserInput {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String roleId;
    private String avatarUrl;
    private String password;

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}