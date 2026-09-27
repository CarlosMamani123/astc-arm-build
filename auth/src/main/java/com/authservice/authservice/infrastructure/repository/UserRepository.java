package com.authservice.authservice.infrastructure.repository;

import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.controller.graphql.dto.UserOutput;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

    // =========================
    // NATIVE: LIST ALL USERS WITH ROLE (1 query LEFT JOIN)
    // =========================
    public List<UserOutput> findAllWithRoleNative() {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager().createNativeQuery(
            "SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.full_name, " +
            "u.phone, u.avatar_url, u.role_id, u.country, r.code AS role_code, r.name AS role_name " +
            "FROM users u LEFT JOIN roles r ON r.id = u.role_id " +
            "ORDER BY u.created_at DESC"
        ).getResultList();

        List<UserOutput> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapRowToUserOutput(row));
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] listUsers (Native LEFT JOIN) -> duration: " + duration + " ms | rows: " + result.size());
        return result;
    }

    // =========================
    // NATIVE: GET USER BY ID WITH ROLE (1 query LEFT JOIN)
    // =========================
    public UserOutput findByIdWithRoleNative(UUID userId) {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager().createNativeQuery(
            "SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.full_name, " +
            "u.phone, u.avatar_url, u.role_id, u.country, r.code AS role_code, r.name AS role_name " +
            "FROM users u LEFT JOIN roles r ON r.id = u.role_id " +
            "WHERE u.id = ?1"
        ).setParameter(1, userId).getResultList();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] getUser (Native LEFT JOIN) -> duration: " + duration + " ms");

        if (rows.isEmpty()) return null;
        return mapRowToUserOutput(rows.get(0));
    }

    // =========================
    // NATIVE: FIND BY EMAIL WITH ROLE (1 query LEFT JOIN for login)
    // =========================
    public Object[] findByEmailWithRoleNative(String email) {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager().createNativeQuery(
            "SELECT u.id, u.username, u.email, u.password_hash, u.first_name, u.last_name, " +
            "u.full_name, u.phone, u.avatar_url, u.role_id, u.country, r.code AS role_code " +
            "FROM users u LEFT JOIN roles r ON r.id = u.role_id " +
            "WHERE u.email = ?1"
        ).setParameter(1, email).getResultList();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] login findByEmail (Native LEFT JOIN) -> duration: " + duration + " ms");

        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    // =========================
    // NATIVE: DELETE BY ID (1 query instead of SELECT+DELETE)
    // =========================
    public boolean deleteByIdNative(UUID userId) {
        long start = System.currentTimeMillis();
        int deleted = getEntityManager().createNativeQuery(
            "DELETE FROM users WHERE id = ?1"
        ).setParameter(1, userId).executeUpdate();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF] deleteUser (Native DELETE) -> duration: " + duration + " ms");
        return deleted > 0;
    }

    // =========================
    // NATIVE: UPDATE + GET WITH ROLE (avoids re-fetching role separately)
    // =========================
    public UserOutput findByIdWithRoleAfterUpdate(UUID userId) {
        return findByIdWithRoleNative(userId);
    }

    // =========================
    // EXISTS CHECKS
    // =========================
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return count("LOWER(email)", email.toLowerCase().trim()) > 0;
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return count("LOWER(username)", username.toLowerCase().trim()) > 0;
    }

    // =========================
    // LEGACY (kept for backward compatibility)
    // =========================
    public Optional<UserEntity> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }



    // =========================
    // ROW MAPPER
    // =========================
    private UserOutput mapRowToUserOutput(Object[] row) {
        UserOutput out = new UserOutput();
        out.setId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
        out.setUsername((String) row[1]);
        out.setEmail((String) row[2]);
        out.setFirstName((String) row[3]);
        out.setLastName((String) row[4]);
        out.setFullName((String) row[5]);
        out.setPhone((String) row[6]);
        out.setAvatarUrl((String) row[7]);
        out.setRoleId(row[8] != null ? UUID.fromString(row[8].toString()) : null);
        out.setCountry((String) row[9]);
        out.setRoleCode((String) row[10]);
        out.setRoleName((String) row[11]);
        return out;
    }
}