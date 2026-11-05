"""
Loads the trained pipelines (training them on first run if the pickles aren't there
yet) and exposes classify(description) for the Kafka consumer and the /predict
debug endpoint to call.

Urgency has a second, deliberately blunt safety net on top of the model: certain
life-risk phrases always force CRITICAL, regardless of what the model predicts.
A missed rescue call is a far worse failure mode than a false alarm, so this
keyword override is not something a smarter model should be allowed to overrule.
"""
import logging
from pathlib import Path

import joblib

from app.config import settings
from app.ml.train import NEED_MODEL_FILE, URGENCY_MODEL_FILE, train
from app.schemas import NeedType, Urgency

logger = logging.getLogger(__name__)

_LIFE_RISK_PHRASES = [
    "trapped", "can't breathe", "cannot breathe", "collapsed", "drowning",
    "capsized", "bleeding heavily", "unconscious", "not breathing",
    "stranded", "rising water", "landslide",
]

_need_model = None
_urgency_model = None


def _ensure_models_loaded() -> None:
    global _need_model, _urgency_model
    if _need_model is not None and _urgency_model is not None:
        return

    artifacts = Path(settings.model_artifacts_dir)
    need_path = artifacts / NEED_MODEL_FILE
    urgency_path = artifacts / URGENCY_MODEL_FILE

    if not need_path.exists() or not urgency_path.exists():
        logger.info("No saved model found at %s — training on the bundled synthetic dataset", artifacts)
        train(settings.model_artifacts_dir)

    _need_model = joblib.load(need_path)
    _urgency_model = joblib.load(urgency_path)


def classify(description: str) -> tuple[NeedType, Urgency, float]:
    """Returns (need_type, urgency, confidence) where confidence is the need-type
    model's own probability for its top prediction — a rough signal, not a calibrated one."""
    _ensure_models_loaded()

    need_probs = _need_model.predict_proba([description])[0]
    need_classes = _need_model.classes_
    top_idx = need_probs.argmax()
    need_type = NeedType(need_classes[top_idx])
    confidence = float(need_probs[top_idx])

    predicted_urgency = Urgency(_urgency_model.predict([description])[0])

    lowered = description.lower()
    if any(phrase in lowered for phrase in _LIFE_RISK_PHRASES):
        urgency = Urgency.CRITICAL
    else:
        urgency = predicted_urgency

    return need_type, urgency, round(confidence, 3)
