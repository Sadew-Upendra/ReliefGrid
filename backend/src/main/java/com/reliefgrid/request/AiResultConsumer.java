package com.reliefgrid.request;

import com.reliefgrid.common.events.RequestEvents.AiClassificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes ai.classification-result, published by the AI service (roadmap Phase 2 —
 * not built yet) after it scores a request's real urgency and need type from free text.
 * Until that service exists, this topic simply has no producer and this listener sits idle;
 * the wiring is here now so the AI service can be added later with zero backend changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiResultConsumer {
    private final ReliefRequestRepository repo;

    @KafkaListener(topics = "ai.classification-result", groupId = "reliefgrid-backend")
    @Transactional
    public void onResult(AiClassificationResult result) {
        repo.findById(result.requestId()).ifPresentOrElse(r -> {
            r.setUrgency(result.urgency());
            r.setNeedType(result.needType());
            log.info("Applied AI classification to request {}: urgency={} needType={} confidence={}",
                    result.requestId(), result.urgency(), result.needType(), result.confidence());
        }, () -> log.warn("Got an AI classification for unknown request {}", result.requestId()));
    }
}
