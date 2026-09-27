package com.authservice.authservice.application.usecase;

import com.authservice.authservice.application.in.LoginUseCase;
import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.repository.UserRepository;
import com.authservice.authservice.infrastructure.security.JwtService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

@ApplicationScoped
public class LoginUseCaseImpl implements LoginUseCase {

    @Inject
    UserRepository userRepository;

    @Inject
    JwtService jwtService;

    @Override
    public String execute(String email, String password) {

        System.out.println("🔥 LOGIN START");
        System.out.println("📩 EMAIL: " + email);

        // 1 SINGLE QUERY: user + role via LEFT JOIN (was 2 roundtrips)
        Object[] row = userRepository.findByEmailWithRoleNative(email);

        if (row == null) {
            throw new RuntimeException("Credenciales inválidas");
        }

        UUID userId = UUID.fromString(row[0].toString());
        String username = (String) row[1];
        String userEmail = (String) row[2];
        String passwordHash = (String) row[3];
        String firstName = (String) row[4];
        String lastName = (String) row[5];
        String roleCode = row[11] != null ? (String) row[11] : "USER";

        System.out.println("✅ USER FOUND: " + userEmail);
        System.out.println("🆔 USER ID: " + userId);

        // 2. Validate password
        boolean valid = BCrypt.checkpw(password, passwordHash);

        System.out.println("🔐 PASSWORD MATCH RESULT: " + valid);

        if (!valid) {
            throw new RuntimeException("Credenciales inválidas");
        }

        System.out.println("🏷 ROLE CODE: " + roleCode);

        // 3. Build a lightweight UserEntity for JWT generation
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(userEmail);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        // 4. Generate JWT (no extra DB query needed)
        String token = jwtService.generateToken(user, roleCode);

        System.out.println("🚀 JWT GENERATED OK");

        return token;
    }
}