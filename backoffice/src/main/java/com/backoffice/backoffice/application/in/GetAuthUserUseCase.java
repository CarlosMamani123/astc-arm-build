package com.backoffice.backoffice.application.in;

import com.backoffice.backoffice.domain.model.User;

public interface GetAuthUserUseCase {
    User execute(String email);
}