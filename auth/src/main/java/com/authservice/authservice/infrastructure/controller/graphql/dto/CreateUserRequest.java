package com.authservice.authservice.infrastructure.controller.graphql.dto;

import org.eclipse.microprofile.graphql.Input;

@Input("CreateUserInput")
public class CreateUserRequest {

    public String id;
    public String username;
    public String email;
    public String password;

    public String firstName;
    public String lastName;
    public String phone;

    public String roleId;
    public String avatarUrl;
    
    public String country;
}