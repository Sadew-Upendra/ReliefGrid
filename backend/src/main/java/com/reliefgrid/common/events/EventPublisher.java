package com.reliefgrid.common.events;

import com.reliefgrid.common.config.KafkaConfig;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper so services never touch KafkaTemplate directly. Publish failures are logged,
 * not thrown: a Kafka outage should degrade ReliefGrid to "AI triage and notifications are
 * delayed," never to "citizens can't submit a request" — see ADR 0003.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    public void requestCreated(RequestEvents.RequestCreated event) {
        send(KafkaConfig.REQUEST_CREATED, event.requestId(), event);
    }

    public void requestStatusChanged(RequestEvents.RequestStatusChanged event) {
        send(KafkaConfig.REQUEST_STATUS_CHANGED, event.requestId(), event);
    }

    public void taskAssigned(RequestEvents.TaskAssigned event) {
        send(KafkaConfig.TASK_ASSIGNED, event.requestId(), event);
    }

    private void send(String topic, UUID key, Object payload) {
        kafka.send(topic, key.toString(), payload).exceptionally(ex -> {
            log.warn("Failed to publish to {} for key {}: {}", topic, key, ex.getMessage());
            return null;
        });
    }
}
