from app.ml.classifier import classify
from app.schemas import NeedType, Urgency


def test_trapped_rescue_is_always_critical():
    need_type, urgency, confidence = classify("A family is trapped on their roof, water still rising")
    assert need_type == NeedType.RESCUE
    assert urgency == Urgency.CRITICAL
    assert 0.0 <= confidence <= 1.0


def test_low_urgency_supply_request_is_not_critical():
    _, urgency, _ = classify("Could use some extra blankets for the shelter, nothing urgent")
    assert urgency != Urgency.CRITICAL


def test_medicine_keywords_classify_as_medicine():
    need_type, _, _ = classify("Diabetic child has run out of insulin, needs it immediately")
    assert need_type == NeedType.MEDICINE


def test_life_risk_phrase_overrides_model_prediction():
    # Phrased blandly enough the model alone might not say CRITICAL, but "trapped" must force it.
    need_type, urgency, _ = classify("A few people are trapped near the old market")
    assert urgency == Urgency.CRITICAL
