package com.reliefgrid.request;

import com.reliefgrid.common.security.AuthUser;
import com.reliefgrid.common.web.PageResponse;
import com.reliefgrid.request.RequestDtos.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse create(@Valid @RequestBody CreateRequest body, @AuthenticationPrincipal AuthUser user) {
        return service.create(body, user);
    }

    @GetMapping
    public PageResponse<RequestResponse> list(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) NeedType needType,
            @RequestParam(required = false) Urgency urgency,
            @RequestParam(required = false) String bbox,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthUser user) {
        return service.search(status, needType, urgency, bbox, pageable, user);
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user) {
        return service.get(id, user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public RequestResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest body,
                                        @AuthenticationPrincipal AuthUser user) {
        return service.transition(id, body.status(), user);
    }

    /** Feeds the AI service's /forecast (Phase 3, demand forecasting) — see docs/forecasting.md. */
    @GetMapping("/daily-counts")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public List<DailyNeedCount> dailyCounts(@RequestParam(defaultValue = "30") int days) {
        int capped = Math.max(1, Math.min(days, 180));
        return service.dailyCountsByNeedType(Instant.now().minus(capped, ChronoUnit.DAYS));
    }
}
