package com.backoffice.backoffice.infrastructure.repository;

import com.backoffice.backoffice.domain.entity.RoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepository {

    @PersistenceContext
    EntityManager em;

    // =========================
    // FIND BY ID
    // =========================
    public RoleEntity findById(UUID id) {
        return em.find(RoleEntity.class, id);
    }

    // =========================
    // FIND BY CODE
    // =========================
    public Optional<RoleEntity> findByCode(String code) {
        return em.createQuery(
                        "SELECT r FROM RoleEntity r WHERE UPPER(r.code) = :code",
                        RoleEntity.class
                )
                .setParameter("code", code.toUpperCase())
                .getResultStream()
                .findFirst();
    }

    // =========================
    // FIND BY ID OR CODE (LO QUE QUIERES)
    // =========================
    public Optional<RoleEntity> findByIdOrCode(String value) {

        // 1. Intentar como UUID
        try {
            UUID uuid = UUID.fromString(value);
            return Optional.ofNullable(findById(uuid));
        } catch (IllegalArgumentException ignored) {
            // no es UUID, seguir como code
        }

        // 2. Buscar por code
        return findByCode(value);
    }
}