package com.backoffice.backoffice.application.in;

import com.backoffice.backoffice.domain.model.User;
import java.util.List;

public interface ListUsersUseCase {
    List<User> execute();
}