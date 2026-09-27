package com.backoffice.backoffice.application.in;

import java.util.UUID;

public interface DeleteUserUseCase {
    boolean execute(UUID id);
}