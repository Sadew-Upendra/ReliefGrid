package com.reliefgrid.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, length = 1000) private String message;
    @Column(nullable = false) private boolean delivered;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
