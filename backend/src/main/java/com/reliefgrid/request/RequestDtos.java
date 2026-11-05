package com.reliefgrid.request;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class RequestDtos {
    private RequestDtos() {}

    public record CreateRequest(
            @NotNull NeedType needType,
            @Min(1) @Max(100000) int quantity,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Size(max = 255) String place,
            @NotBlank @Size(max = 2000) String description,
            boolean lifeAtRisk) {}

    public record UpdateStatusRequest(@NotNull RequestStatus status) {}

    public record RequestResponse(UUID id, NeedType needType, int quantity, Urgency urgency, RequestStatus status,
                                  double lat, double lng, String place, String description, Instant createdAt) {
        public static RequestResponse from(ReliefRequest r) {
            return new RequestResponse(r.getId(), r.getNeedType(), r.getQuantity(), r.getUrgency(), r.getStatus(),
                    r.getLat(), r.getLng(), r.getPlace(), r.getDescription(), r.getCreatedAt());
        }
    }
}
