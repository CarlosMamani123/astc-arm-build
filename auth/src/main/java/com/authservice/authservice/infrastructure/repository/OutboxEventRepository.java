package com.authservice.authservice.infrastructure.repository;

import com.authservice.authservice.domain.entity.OutboxEventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEventEntity> {

    public List<OutboxEventEntity> findUnprocessed(int limit) {
        return find("processedAt is null")
                .page(0, limit)
                .list();
    }
}