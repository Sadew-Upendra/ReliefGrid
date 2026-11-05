"""Pydantic mirrors of the backend's Java records in RequestEvents.java.
Field names and shapes must stay in sync with that file — this is the wire contract."""
from datetime import datetime
from enum import Enum

from pydantic import BaseModel


class NeedType(str, Enum):
    FOOD = "FOOD"
    WATER = "WATER"
    MEDICINE = "MEDICINE"
    SHELTER = "SHELTER"
    RESCUE = "RESCUE"
    OTHER = "OTHER"


class Urgency(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class RequestCreated(BaseModel):
    requestId: str
    needType: NeedType
    quantity: int
    lat: float
    lng: float
    description: str
    createdAt: datetime


class AiClassificationResult(BaseModel):
    requestId: str
    urgency: Urgency
    needType: NeedType
    confidence: float
