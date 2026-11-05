package com.reliefgrid.common.events;

import com.reliefgrid.request.NeedType;
import com.reliefgrid.request.RequestStatus;
import com.reliefgrid.request.Urgency;
import java.time.Instant;
import java.util.UUID;

/**
 * Payloads published to Kafka. Kept as simple records — this is the wire contract with
 * the future AI service (and anything else that consumes these topics), so changing a
 * field here is a breaking change for consumers, not just an internal refactor.
 */
public final class RequestEvents {
    private RequestEvents() {}

    public record RequestCreated(UUID requestId, NeedType needType, int quantity, double lat, double lng,
                                 String description, Instant createdAt) {}

    public record RequestStatusChanged(UUID requestId, RequestStatus from, RequestStatus to, Instant changedAt) {}

    public record TaskAssigned(UUID taskId, UUID requestId, UUID volunteerId, Instant assignedAt) {}

    /** Consumed, not produced, by this service — published by the AI service once it exists (roadmap Phase 2). */
    public record AiClassificationResult(UUID requestId, Urgency urgency, NeedType needType, double confidence) {}
}
