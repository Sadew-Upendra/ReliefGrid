package com.reliefgrid.inventory;

import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.inventory.InventoryDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/supply-points")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;

    @GetMapping
    public List<SupplyPointResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('NGO','ADMIN')")
    public SupplyPointResponse create(@Valid @RequestBody CreateSupplyPoint body, @AuthenticationPrincipal AuthUser user) {
        return service.create(body, user);
    }

    @PutMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('NGO','ADMIN')")
    public SupplyPointResponse updateInventory(@PathVariable UUID id, @Valid @RequestBody UpdateInventory body,
                                               @AuthenticationPrincipal AuthUser user) {
        return service.updateInventory(id, body, user);
    }
}
