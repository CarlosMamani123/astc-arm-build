package com.backoffice.backoffice.application.in;

import java.util.UUID;

public interface DeleteUserAvatarUseCase {

    void execute(UUID userId);
}