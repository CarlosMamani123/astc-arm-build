package com.smms.assistance.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "justification_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JustificationHistoryEntity extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(name = "justification_id", nullable = false)
    private UUID justificationId;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    private String comment;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime();
        }
    }
}
