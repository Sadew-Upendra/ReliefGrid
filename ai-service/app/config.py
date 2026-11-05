"""Runtime configuration, all overridable via environment variables (see .env.example)."""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(case_sensitive=False)

    kafka_bootstrap_servers: str = "localhost:9092"
    topic_request_created: str = "request.created"
    topic_classification_result: str = "ai.classification-result"
    consumer_group_id: str = "reliefgrid-ai-service"
    model_artifacts_dir: str = "app/ml/artifacts"


settings = Settings()
