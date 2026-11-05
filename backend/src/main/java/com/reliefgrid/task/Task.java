package com.reliefgrid.task;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID requestId;
    private UUID volunteerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TaskStatus status = TaskStatus.OPEN;
    /** Optimistic lock: two volunteers accepting at once cannot both win. */
    @Version private Long version;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    private Instant acceptedAt;
    private Instant completedAt;
}
