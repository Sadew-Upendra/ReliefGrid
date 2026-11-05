# ReliefGrid AI Service

FastAPI + scikit-learn. Consumes `request.created` from Kafka, classifies each report's
real urgency and need type from its free-text description, and publishes the result to
`ai.classification-result`, which the backend (`AiResultConsumer`, wired since Step 10)
applies to the request automatically.

## Why scikit-learn, not a large language model
This is a small, fast, fully-offline classifier — no GPU, no API calls, sub-millisecond
inference. That fits ReliefGrid's actual need (triage a short incoming report, immediately)
better than an LLM call would, and it's honest about being a first version: `app/ml/training_data.py`
is a small hand-written synthetic dataset, meant to be replaced with real labeled reports
once ReliefGrid has some. Nothing outside `training_data.py` needs to change when that happens.

## Safety net on top of the model
A fixed list of life-risk phrases ("trapped", "drowning", "can't breathe", ...) always forces
`CRITICAL` regardless of what the model predicts — see the comment in `app/ml/classifier.py`.
A missed emergency is a far worse failure than a false alarm.

## Run locally
```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env   # edit KAFKA_BOOTSTRAP_SERVERS if not using the default
uvicorn app.main:app --reload
```
Needs a running Kafka broker to actually consume anything — see `infra/kafka/docker-compose.kafka.yml`.

Try the classifier directly, without Kafka:
```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "Six people trapped on a rooftop, water still rising"}'
```

## Tests
```bash
pytest
```

## Retraining
```bash
python -m app.ml.train
```
Regenerates `app/ml/artifacts/*.joblib` from `training_data.py`. The Docker image also runs
this at build time, so a fresh container never needs to train on startup.
