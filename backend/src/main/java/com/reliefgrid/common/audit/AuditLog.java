package com.reliefgrid.common.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID actorId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String entityType;
    private UUID entityId;
    private String details;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
