package com.reliefgrid.common.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Services publish plain Spring application events inside their @Transactional methods.
 * This relay only forwards them to Kafka AFTER_COMMIT, so a request that gets rolled back
 * (e.g. a later validation failure in the same transaction) never produces a Kafka message
 * for data that was never actually saved.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventRelay {
    private final EventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestCreated(RequestEvents.RequestCreated event) {
        publisher.requestCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestStatusChanged(RequestEvents.RequestStatusChanged event) {
        publisher.requestStatusChanged(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(RequestEvents.TaskAssigned event) {
        publisher.taskAssigned(event);
    }
}
