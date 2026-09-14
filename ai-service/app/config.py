from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "ai-meeting-knowledge-ai-service"
    environment: str = "development"
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "info"

    # Speech-to-Text configuration
    whisper_model: str = "tiny"
    max_upload_size_bytes: int = 52428800  # 50 MB
    allowed_content_types: list[str] = [
        "audio/mpeg",
        "audio/mp3",
        "audio/wav",
        "audio/x-wav",
        "audio/mp4",
        "audio/m4a",
        "audio/x-m4a",
        "audio/webm",
        "audio/ogg",
    ]

    # LLM configuration
    openrouter_api_key: str = ""
    openrouter_base_url: str = "https://openrouter.ai/api/v1"
    openrouter_model: str = "meta-llama/llama-3.3-70b-instruct:free"
    openrouter_timeout_seconds: float = 60.0
    max_analysis_transcript_chars: int = 100000


    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
