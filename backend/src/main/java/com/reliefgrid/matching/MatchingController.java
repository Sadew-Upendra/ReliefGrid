package com.reliefgrid.matching;

import com.reliefgrid.inventory.NearestSupply;
import com.reliefgrid.inventory.SupplyPointRepository;
import com.reliefgrid.request.ReliefRequest;
import com.reliefgrid.request.RequestService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests/{id}/matches")
@RequiredArgsConstructor
public class MatchingController {
    private final RequestService requests;
    private final SupplyPointRepository supply;

    public record SupplyMatch(UUID supplyPointId, String name, double lat, double lng, int available, long distanceMeters) {}

    /** Nearest supply points that hold the needed item, closest first. */
    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public List<SupplyMatch> matches(@PathVariable UUID id, @RequestParam(defaultValue = "5") int limit) {
        ReliefRequest r = requests.find(id);
        int capped = Math.max(1, Math.min(limit, 20));
        return supply.findNearest(r.getLat(), r.getLng(), r.getNeedType().name(), capped).stream()
                .map((NearestSupply n) -> new SupplyMatch(n.getId(), n.getName(), n.getLat(), n.getLng(),
                        n.getAvailable(), Math.round(n.getDistance())))
                .toList();
    }
}
