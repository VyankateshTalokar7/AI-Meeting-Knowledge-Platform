import logging
from typing import Any
import whisper
from app.config import get_settings

logger = logging.getLogger("ai-service.transcription")


class TranscriptionError(Exception):
    """Custom exception raised when audio transcription fails."""
    pass


class TranscriptionService:
    def __init__(self) -> None:
        self._models: dict[str, Any] = {}

    def get_model(self) -> Any:
        settings = get_settings()
        target_model = settings.whisper_model
        if target_model not in self._models:
            logger.info("Loading Whisper model '%s'...", target_model)
            try:
                self._models[target_model] = whisper.load_model(target_model)
                logger.info("Whisper model '%s' loaded successfully.", target_model)
            except Exception as exc:
                logger.error("Failed to load Whisper model '%s': %s", target_model, exc)
                raise TranscriptionError(f"Failed to load transcription model '{target_model}'.") from exc
        return self._models[target_model]

    def transcribe_audio(self, file_path: str) -> dict[str, Any]:
        try:
            model = self.get_model()
            logger.info("Transcribing audio file...")
            result = model.transcribe(file_path)
            
            segments = []
            for seg in result.get("segments", []):
                segments.append({
                    "id": seg.get("id"),
                    "start": round(seg.get("start", 0.0), 2),
                    "end": round(seg.get("end", 0.0), 2),
                    "text": seg.get("text", "").strip(),
                })

            return {
                "text": result.get("text", "").strip(),
                "language": result.get("language", ""),
                "segments": segments,
            }
        except TranscriptionError:
            raise
        except Exception as exc:
            logger.error("Error transcribing audio file: %s", exc)
            raise TranscriptionError("An error occurred during audio transcription.") from exc



transcription_service = TranscriptionService()
