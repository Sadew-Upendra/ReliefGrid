import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel

from app.forecasting.forecaster import DailyCount, NeedTypeForecast, forecast
from app.kafka import consumer
from app.ml.classifier import classify
from app.schemas import NeedType, Urgency

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting request.created consumer")
    consumer.start()
    yield
    logger.info("Shutting down request.created consumer")
    consumer.stop()


app = FastAPI(title="ReliefGrid AI Service", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health():
    """Plain liveness/readiness check — the Kafka consumer runs in a background
    thread, so a 200 here means the process is up, not that Kafka is reachable."""
    return {"status": "ok"}


class PredictRequest(BaseModel):
    description: str


class PredictResponse(BaseModel):
    needType: NeedType
    urgency: Urgency
    confidence: float


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    """Manual/debug endpoint — lets you try the classifier over HTTP without
    needing a Kafka message. Not used by the backend; the consumer is the real path."""
    need_type, urgency, confidence = classify(req.description)
    return PredictResponse(needType=need_type, urgency=urgency, confidence=confidence)


class ForecastRequest(BaseModel):
    dailyCounts: list[DailyCount]
    horizonDays: int = 7


@app.post("/forecast", response_model=list[NeedTypeForecast])
def forecast_demand(req: ForecastRequest):
    """Called on demand by a coordinator/admin dashboard (Phase 3) — not consumed from
    Kafka, since a forecast is cheap to compute and doesn't need to react to every event.
    Feed it GET /api/v1/requests/daily-counts from the backend (coordinator/admin only)."""
    capped_horizon = max(1, min(req.horizonDays, 30))
    return forecast(req.dailyCounts, capped_horizon)
