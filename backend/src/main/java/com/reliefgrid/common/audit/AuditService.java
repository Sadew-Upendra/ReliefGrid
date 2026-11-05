package com.reliefgrid.common.audit;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Joins the caller's transaction, so a change and its audit entry commit or roll back together. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void record(UUID actorId, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        repo.save(log);
    }
}
