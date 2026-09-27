package com.backoffice.backoffice.application.in;

import com.backoffice.backoffice.domain.model.User;
import java.util.UUID;

public interface GetUserDetailUseCase {
    User execute(UUID id);
}