package com.authservice.authservice.infrastructure.controller.graphql.dto;

public class LoginOutput {
    public String token;

    public LoginOutput(String token) {
        this.token = token;
    }
}