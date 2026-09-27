package com.authservice.authservice.application.in;

public interface LoginUseCase {
    String execute(String email, String password);
}