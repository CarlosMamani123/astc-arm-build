package com.backoffice.backoffice.application.in;

import com.backoffice.backoffice.domain.model.User;

public interface GetUserByEmailUseCase {
    User execute(String email);
}