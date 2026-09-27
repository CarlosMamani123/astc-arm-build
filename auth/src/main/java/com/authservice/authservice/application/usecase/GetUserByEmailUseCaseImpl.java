package com.authservice.authservice.application.usecase;

import com.authservice.authservice.application.in.GetUserByEmailUseCase;
import com.authservice.authservice.domain.model.User;
import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.repository.UserRepository;
import com.authservice.authservice.application.usecase.UserMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetUserByEmailUseCaseImpl implements GetUserByEmailUseCase {

    @Inject
    UserRepository userRepository;

    @Override
    public User execute(String email) {

        return userRepository.findByEmail(email)
                .map(UserMapper::toDomain)
                .orElse(null);
    }
}