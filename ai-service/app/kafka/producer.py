import json
import logging

from confluent_kafka import Producer

from app.config import settings

logger = logging.getLogger(__name__)

_producer: Producer | None = None


def get_producer() -> Producer:
    global _producer
    if _producer is None:
        _producer = Producer({"bootstrap.servers": settings.kafka_bootstrap_servers, "acks": "all"})
    return _producer


def _delivery_callback(err, msg) -> None:
    if err is not None:
        logger.warning("Delivery failed for key %s: %s", msg.key(), err)


def publish_classification_result(result) -> None:
    """result: app.schemas.AiClassificationResult"""
    try:
        producer = get_producer()
        producer.produce(
            settings.topic_classification_result,
            key=result.requestId.encode("utf-8"),
            value=json.dumps(result.model_dump(mode="json")).encode("utf-8"),
            callback=_delivery_callback,
        )
        producer.poll(0)  # trigger delivery callbacks for previously produced messages, non-blocking
    except Exception:
        # Same philosophy as the backend's EventPublisher: a Kafka hiccup here means
        # this request's urgency just doesn't get refined — never a hard failure.
        logger.exception("Failed to publish classification result for request %s", result.requestId)
