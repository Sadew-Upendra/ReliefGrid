package com.reliefgrid.request;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "relief_requests")
@Getter @Setter @NoArgsConstructor
public class ReliefRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID reporterId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NeedType needType;
    @Column(nullable = false) private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Urgency urgency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RequestStatus status;
    @Column(nullable = false) private double lat;
    @Column(nullable = false) private double lng;
    private String place;
    @Column(nullable = false, length = 2000) private String description;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
}
