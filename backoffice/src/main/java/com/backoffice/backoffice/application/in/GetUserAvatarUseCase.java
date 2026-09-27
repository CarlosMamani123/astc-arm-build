package com.backoffice.backoffice.application.in;

import java.util.UUID;

public interface GetUserAvatarUseCase {
    String execute(UUID userId);
}