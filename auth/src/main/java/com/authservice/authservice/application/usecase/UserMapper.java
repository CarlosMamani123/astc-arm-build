package com.authservice.authservice.application.usecase;

import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.domain.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class UserMapper {

    // =========================
    // ENTITY → DOMAIN
    // =========================
    public static User toDomain(UserEntity e) {
        if (e == null) return null;

        User u = new User();

        u.setId(e.getId());
        u.setUsername(e.getUsername());
        u.setEmail(e.getEmail());
        u.setFirstName(e.getFirstName());
        u.setLastName(e.getLastName());

        u.setPhone(e.getPhone());
        u.setAvatarUrl(e.getAvatarUrl());
        u.setRoleId(e.getRoleId());

        return u;
    }

    // =========================
    // DOMAIN → ENTITY
    // =========================
    public static UserEntity toEntity(User u) {
        if (u == null) return null;

        UserEntity e = new UserEntity();

        e.setId(u.getId());

        e.setUsername(u.getUsername());
        e.setEmail(u.getEmail());

        e.setFirstName(u.getFirstName());
        e.setLastName(u.getLastName());

        e.setFullName(buildFullName(u.getFirstName(), u.getLastName()));

        e.setPhone(u.getPhone());
        e.setAvatarUrl(u.getAvatarUrl());
        e.setRoleId(u.getRoleId());

        return e;
    }

    // =========================
    // PASSWORD HASH (NUEVO)
    // =========================
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) return null;
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    private static String buildFullName(String first, String last) {
        if (first == null && last == null) return null;
        if (first == null) return last;
        if (last == null) return first;
        return first + " " + last;
    }
}