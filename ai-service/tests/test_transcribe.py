import io
import os
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient
from app.main import app
from app.services.transcription import TranscriptionError

client = TestClient(app)


def test_health_endpoint_still_works():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_transcribe_unsupported_file_type():
    file_content = b"This is a plain text file, not audio."
    files = {"file": ("document.txt", io.BytesIO(file_content), "text/plain")}
    response = client.post("/transcribe", files=files)
    assert response.status_code == 400
    data = response.json()
    assert "Unsupported file type" in data["detail"]


def test_transcribe_empty_file():
    empty_content = b""
    files = {"file": ("empty.wav", io.BytesIO(empty_content), "audio/wav")}
    response = client.post("/transcribe", files=files)
    assert response.status_code == 400
    data = response.json()
    assert "Uploaded file is empty" in data["detail"]


def test_transcribe_file_exceeds_size_limit():
    dummy_wav = b"12345678901234567890"  # 20 bytes
    mock_settings = MagicMock()
    mock_settings.allowed_content_types = ["audio/wav"]
    mock_settings.max_upload_size_bytes = 10  # 10 byte limit for test

    with patch("app.routers.transcribe.get_settings", return_value=mock_settings):
        files = {"file": ("large.wav", io.BytesIO(dummy_wav), "audio/wav")}
        response = client.post("/transcribe", files=files)

        assert response.status_code == 400
        data = response.json()
        assert "File size exceeds maximum limit" in data["detail"]


def test_transcribe_success_mocked():
    mock_transcription_result = {
        "text": "Hello and welcome to the AI meeting knowledge platform test.",
        "language": "en",
        "segments": [
            {
                "id": 0,
                "start": 0.0,
                "end": 2.5,
                "text": "Hello and welcome to the AI meeting knowledge platform test.",
            }
        ],
    }

    dummy_wav_content = b"RIFF....WAVEfmt ....data...."

    with patch(
        "app.routers.transcribe.transcription_service.transcribe_audio",
        return_value=mock_transcription_result,
    ) as mock_transcribe:
        files = {"file": ("test_sample.wav", io.BytesIO(dummy_wav_content), "audio/wav")}
        response = client.post("/transcribe", files=files)

        assert response.status_code == 200
        data = response.json()
        assert data["filename"] == "test_sample.wav"
        assert data["text"] == "Hello and welcome to the AI meeting knowledge platform test."
        assert data["language"] == "en"
        assert len(data["segments"]) == 1
        assert mock_transcribe.called


def test_temp_file_cleanup_on_success():
    captured_paths = []

    def mock_transcribe(file_path):
        captured_paths.append(file_path)
        assert os.path.exists(file_path), "Temp file should exist during transcription"
        return {"text": "Clean test", "language": "en", "segments": []}

    dummy_wav_content = b"RIFF....WAVEfmt ....data...."

    with patch(
        "app.routers.transcribe.transcription_service.transcribe_audio",
        side_effect=mock_transcribe,
    ):
        files = {"file": ("sample.wav", io.BytesIO(dummy_wav_content), "audio/wav")}
        response = client.post("/transcribe", files=files)

        assert response.status_code == 200
        assert len(captured_paths) == 1
        assert not os.path.exists(captured_paths[0]), "Temp file must be cleaned up after success"


def test_temp_file_cleanup_on_failure():
    captured_paths = []

    def mock_transcribe_fail(file_path):
        captured_paths.append(file_path)
        assert os.path.exists(file_path), "Temp file should exist during transcription"
        raise TranscriptionError("Simulated transcription failure")

    dummy_wav_content = b"RIFF....WAVEfmt ....data...."

    with patch(
        "app.routers.transcribe.transcription_service.transcribe_audio",
        side_effect=mock_transcribe_fail,
    ):
        files = {"file": ("sample.wav", io.BytesIO(dummy_wav_content), "audio/wav")}
        response = client.post("/transcribe", files=files)

        assert response.status_code == 500
        assert len(captured_paths) == 1
        assert not os.path.exists(captured_paths[0]), "Temp file must be cleaned up after failure"


def test_transcribe_service_error_handling():
    dummy_audio = b"dummy audio content"

    with patch(
        "app.routers.transcribe.transcription_service.transcribe_audio",
        side_effect=TranscriptionError("Model processing failed"),
    ):
        files = {"file": ("test_sample.mp3", io.BytesIO(dummy_audio), "audio/mpeg")}
        response = client.post("/transcribe", files=files)

        assert response.status_code == 500
        data = response.json()
        assert data["detail"] == "Audio transcription failed."
        # Confirm internal filesystem paths or exceptions are NOT exposed
        assert "Model processing failed" not in data["detail"]
