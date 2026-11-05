package com.reliefgrid.inventory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "supply_points")
@Getter @Setter @NoArgsConstructor
public class SupplyPoint {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private double lat;
    @Column(nullable = false) private double lng;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "supplyPoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryItem> items = new ArrayList<>();
}
