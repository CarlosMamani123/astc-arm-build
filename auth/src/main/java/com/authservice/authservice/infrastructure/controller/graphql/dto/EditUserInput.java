package com.authservice.authservice.infrastructure.controller.graphql.dto;

import org.eclipse.microprofile.graphql.Input;

@Input
public class EditUserInput {

    public String username;
    public String email;
    public String roleId;

    public String firstName;
    public String lastName;
    public String phone;

    public String avatarUrl;
    
    public String country;

    public String password; // opcional
}