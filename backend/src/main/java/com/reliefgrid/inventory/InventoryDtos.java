package com.reliefgrid.inventory;

import com.reliefgrid.request.NeedType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record CreateSupplyPoint(
            @NotBlank @Size(max = 150) String name,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {}

    public record InventoryEntry(@NotNull NeedType needType, @Min(0) @Max(10000000) int quantity) {}

    public record UpdateInventory(@NotNull @Valid List<@Valid InventoryEntry> items) {}

    public record SupplyPointResponse(UUID id, String name, double lat, double lng, List<InventoryEntry> items) {
        public static SupplyPointResponse from(SupplyPoint s) {
            return new SupplyPointResponse(s.getId(), s.getName(), s.getLat(), s.getLng(),
                    s.getItems().stream().map(i -> new InventoryEntry(i.getNeedType(), i.getQuantity())).toList());
        }
    }
}
