package com.authservice.authservice.infrastructure.repository;

import com.authservice.authservice.domain.entity.RoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepository {

    @PersistenceContext
    EntityManager em;

    public RoleEntity findById(UUID id) {
        return em.find(RoleEntity.class, id);
    }

    public java.util.List<RoleEntity> listAll() {
        return em.createQuery("SELECT r FROM RoleEntity r", RoleEntity.class).getResultList();
    }

    public Optional<RoleEntity> findByCode(String code) {
        return em.createQuery(
                        "SELECT r FROM RoleEntity r WHERE UPPER(r.code) = :code",
                        RoleEntity.class
                )
                .setParameter("code", code.toUpperCase())
                .getResultStream()
                .findFirst();
    }
}