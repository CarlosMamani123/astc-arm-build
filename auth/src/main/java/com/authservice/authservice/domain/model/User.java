package com.authservice.authservice.domain.model;

import java.util.UUID;

public class User {

    private UUID id;

    private String username;
    private String email;
    private String password;

    private String firstName;
    private String lastName;
    private String fullName;

    private String phone;
    private String avatarUrl;

    private UUID roleId;

    private String country;

    // 🔥 GETTERS & SETTERS

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}