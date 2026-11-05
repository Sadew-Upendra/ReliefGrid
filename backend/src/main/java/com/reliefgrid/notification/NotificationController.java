package com.reliefgrid.notification;

import com.reliefgrid.common.security.AuthUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;

    public record NotificationResponse(UUID id, String subject, String message, boolean delivered, Instant createdAt) {}

    /** Always scoped to the caller — there is no "view anyone else's notifications" mode. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthUser user) {
        return repo.findByUserIdOrderByCreatedAtDesc(user.id(), pageable).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getSubject(), n.getMessage(), n.isDelivered(), n.getCreatedAt()))
                .toList();
    }
}
