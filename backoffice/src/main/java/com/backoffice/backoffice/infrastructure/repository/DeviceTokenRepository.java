package com.backoffice.backoffice.infrastructure.repository;

import com.backoffice.backoffice.domain.notification.DeviceTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeviceTokenRepository implements PanacheRepositoryBase<DeviceTokenEntity, UUID> {

    public List<DeviceTokenEntity> findByUserId(UUID userId) {
        return find("userId = ?1", userId).list();
    }

    public DeviceTokenEntity findByToken(String token) {
        return find("token = ?1", token).firstResult();
    }
}
