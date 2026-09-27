package com.authservice.authservice.application.in;

import com.authservice.authservice.domain.model.User;

public interface GetAuthUserUseCase {
    User execute(String email);
}