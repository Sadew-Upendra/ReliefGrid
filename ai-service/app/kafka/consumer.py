import json
import logging
import threading

from confluent_kafka import Consumer

from app.config import settings
from app.kafka.producer import publish_classification_result
from app.ml.classifier import classify
from app.schemas import AiClassificationResult, RequestCreated

logger = logging.getLogger(__name__)

_stop_event = threading.Event()
_thread: threading.Thread | None = None


def _consume_loop() -> None:
    consumer = Consumer({
        "bootstrap.servers": settings.kafka_bootstrap_servers,
        "group.id": settings.consumer_group_id,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": True,
    })
    consumer.subscribe([settings.topic_request_created])
    logger.info("Listening on topic '%s' as group '%s'", settings.topic_request_created, settings.consumer_group_id)

    try:
        while not _stop_event.is_set():
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                logger.warning("Kafka error: %s", msg.error())
                continue
            try:
                event = RequestCreated.model_validate(json.loads(msg.value().decode("utf-8")))
                need_type, urgency, confidence = classify(event.description)
                result = AiClassificationResult(
                    requestId=event.requestId, urgency=urgency, needType=need_type, confidence=confidence
                )
                publish_classification_result(result)
                logger.info(
                    "Classified request %s -> needType=%s urgency=%s confidence=%s",
                    event.requestId, need_type, urgency, confidence,
                )
            except Exception:
                # One bad/malformed message should never kill the consumer loop for everyone else.
                logger.exception("Failed to process message: %s", msg.value())
    finally:
        consumer.close()
        logger.info("Consumer loop stopped")


def start() -> None:
    global _thread
    if _thread is not None and _thread.is_alive():
        return
    _stop_event.clear()
    _thread = threading.Thread(target=_consume_loop, name="request-created-consumer", daemon=True)
    _thread.start()


def stop() -> None:
    _stop_event.set()
    if _thread is not None:
        _thread.join(timeout=5)
