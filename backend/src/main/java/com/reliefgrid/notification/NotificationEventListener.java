package com.reliefgrid.notification;

import com.reliefgrid.common.events.RequestEvents.RequestStatusChanged;
import com.reliefgrid.common.events.RequestEvents.TaskAssigned;
import com.reliefgrid.request.ReliefRequest;
import com.reliefgrid.request.ReliefRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the same two topics the backend itself publishes (request.status-changed,
 * task.assigned) — see docs/architecture.md's "Kafka Topics" table. Kept as a consumer
 * of Kafka messages rather than a direct in-process call from RequestService/TaskService
 * so notification delivery can be scaled, retried, or moved to its own service later
 * without touching the request/task code at all.
 *
 * request.status-changed -> notify the reporter (what happened to their request).
 * task.assigned -> notify the volunteer (confirmation of what they just accepted) —
 * the reporter already gets a status-changed notification for the same event, so this
 * intentionally does not duplicate that message to them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationService notifications;
    private final ReliefRequestRepository requests;

    @KafkaListener(topics = "request.status-changed", groupId = "reliefgrid-backend-notifications")
    public void onRequestStatusChanged(RequestStatusChanged event) {
        requests.findById(event.requestId()).ifPresentOrElse(request -> {
            String subject = "Update on your " + request.getNeedType() + " request";
            String message = switch (event.to()) {
                case VERIFIED -> "Your request has been verified and is being matched with help nearby.";
                case ASSIGNED -> "A volunteer has been assigned to your request and is on the way.";
                case FULFILLED -> "Your request has been marked fulfilled. Thank you, and stay safe.";
                case REJECTED -> "Your request could not be verified. Please contact a coordinator if this is urgent.";
                case PENDING -> "Your request is waiting for review.";
            };
            notifications.notifyUser(request.getReporterId(), subject, message);
        }, () -> log.warn("Status-changed event for unknown request {}", event.requestId()));
    }

    @KafkaListener(topics = "task.assigned", groupId = "reliefgrid-backend-notifications")
    public void onTaskAssigned(TaskAssigned event) {
        ReliefRequest request = requests.findById(event.requestId()).orElse(null);
        if (request == null) {
            log.warn("Task-assigned event for unknown request {}", event.requestId());
            return;
        }
        String subject = "You're confirmed for a " + request.getNeedType() + " request";
        String message = "Thanks for volunteering. Location: " + request.getPlace()
                + ". Details: " + request.getDescription();
        notifications.notifyUser(event.volunteerId(), subject, message);
    }
}
