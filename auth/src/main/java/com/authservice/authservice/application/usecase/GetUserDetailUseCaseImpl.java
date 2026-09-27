package com.authservice.authservice.application.usecase;

import com.authservice.authservice.application.in.GetUserDetailUseCase;
import com.authservice.authservice.domain.model.User;
import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.repository.UserRepository;
import com.authservice.authservice.application.usecase.UserMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetUserDetailUseCaseImpl implements GetUserDetailUseCase {

    @Inject
    UserRepository userRepository;

    @Override
    public User execute(UUID id) {

        UserEntity entity = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserMapper.toDomain(entity);
    }
}