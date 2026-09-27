package com.authservice.authservice.application.in;

import com.authservice.authservice.domain.model.User;
import java.util.UUID;

public interface GetUserDetailUseCase {
    User execute(UUID id);
}