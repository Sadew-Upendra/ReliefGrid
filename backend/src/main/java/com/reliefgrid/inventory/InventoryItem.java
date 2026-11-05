package com.reliefgrid.inventory;

import com.reliefgrid.request.NeedType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor
public class InventoryItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supply_point_id")
    private SupplyPoint supplyPoint;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NeedType needType;
    @Column(nullable = false) private int quantity;
}
