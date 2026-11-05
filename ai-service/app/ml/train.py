"""
Trains two small TF-IDF + Logistic Regression pipelines (need type, urgency) on the
synthetic dataset in training_data.py and pickles them to app/ml/artifacts/.

Run directly to (re)train:  python -m app.ml.train
classifier.py also calls this automatically the first time the app starts and finds
no saved model — so a fresh checkout works with zero manual setup steps.
"""
import logging
from pathlib import Path

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

from app.ml.training_data import DESCRIPTIONS, NEED_LABELS, URGENCY_LABELS

logger = logging.getLogger(__name__)

NEED_MODEL_FILE = "need_type_model.joblib"
URGENCY_MODEL_FILE = "urgency_model.joblib"


def _make_pipeline() -> Pipeline:
    return Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=1, sublinear_tf=True)),
        ("clf", LogisticRegression(max_iter=1000, class_weight="balanced")),
    ])


def train(artifacts_dir: str) -> None:
    out = Path(artifacts_dir)
    out.mkdir(parents=True, exist_ok=True)

    need_model = _make_pipeline().fit(DESCRIPTIONS, NEED_LABELS)
    joblib.dump(need_model, out / NEED_MODEL_FILE)

    urgency_model = _make_pipeline().fit(DESCRIPTIONS, URGENCY_LABELS)
    joblib.dump(urgency_model, out / URGENCY_MODEL_FILE)

    logger.info("Trained and saved need-type and urgency models to %s", out)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    train("app/ml/artifacts")
