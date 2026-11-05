package com.reliefgrid.inventory;

import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.inventory.InventoryDtos.*;
import com.reliefgrid.user.Role;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final SupplyPointRepository repo;
    private final AuditService audit;

    @Transactional
    public SupplyPointResponse create(CreateSupplyPoint d, AuthUser user) {
        SupplyPoint s = new SupplyPoint();
        s.setOwnerId(user.id());
        s.setName(d.name().trim());
        s.setLat(d.lat());
        s.setLng(d.lng());
        repo.save(s);
        audit.record(user.id(), "SUPPLY_POINT_CREATED", "SUPPLY_POINT", s.getId(), s.getName());
        return SupplyPointResponse.from(s);
    }

    @Transactional(readOnly = true)
    public List<SupplyPointResponse> list() {
        return repo.findAllWithItems().stream().map(SupplyPointResponse::from).toList();
    }

    @Transactional
    public SupplyPointResponse updateInventory(UUID id, UpdateInventory d, AuthUser user) {
        SupplyPoint s = repo.findById(id).orElseThrow(() -> ApiException.notFound("Supply point"));
        if (user.role() != Role.ADMIN && !s.getOwnerId().equals(user.id())) {
            throw ApiException.forbidden("You can only update your own supply points");
        }
        for (InventoryEntry e : d.items()) {
            InventoryItem item = s.getItems().stream().filter(i -> i.getNeedType() == e.needType()).findFirst().orElseGet(() -> {
                InventoryItem n = new InventoryItem();
                n.setSupplyPoint(s);
                n.setNeedType(e.needType());
                s.getItems().add(n);
                return n;
            });
            item.setQuantity(e.quantity());
        }
        audit.record(user.id(), "INVENTORY_UPDATED", "SUPPLY_POINT", s.getId(), d.items().size() + " item(s)");
        return SupplyPointResponse.from(s);
    }
}
