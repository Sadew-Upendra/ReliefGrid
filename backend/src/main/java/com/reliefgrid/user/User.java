package com.reliefgrid.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
