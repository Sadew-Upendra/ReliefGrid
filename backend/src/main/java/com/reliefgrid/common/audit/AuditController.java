package com.reliefgrid.common.audit;

import com.reliefgrid.common.web.PageResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository repo;

    public record AuditLogResponse(UUID id, UUID actorId, String action, String entityType, UUID entityId, String details, Instant createdAt) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(@PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(repo.findAll(pageable).map(a -> new AuditLogResponse(
                a.getId(), a.getActorId(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())));
    }
}
