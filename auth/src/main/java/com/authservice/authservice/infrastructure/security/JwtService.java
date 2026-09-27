package com.authservice.authservice.infrastructure.security;

import com.authservice.authservice.domain.entity.RoleEntity;
import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.repository.RoleRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    private static final String ISSUER = "auth-service";

    @Inject
    RoleRepository roleRepository;

    public String generateToken(UserEntity user, String roleCode) {

        var builder = Jwt.issuer(ISSUER)
                .subject(user.getId().toString())
                .groups(Set.of(roleCode))
                .claim("role", roleCode)
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .expiresIn(Duration.ofHours(2));

        return builder.sign();
    }

    private String resolveRole(UserEntity user) {
        RoleEntity role = roleRepository.findById(user.getRoleId());
        return (role != null) ? role.getCode() : "USER";
    }
}