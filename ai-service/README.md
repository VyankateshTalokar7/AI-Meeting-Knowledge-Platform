# AI Service — Speech-to-Text & Processing Microservice

FastAPI microservice for handling Speech-to-Text audio transcription (using OpenAI Whisper) and future AI knowledge processing for the AI Meeting Knowledge Platform.

## Prerequisites

- **Python 3.12+**
- **FFmpeg** (Required for audio decoding by Whisper):
  - macOS: `brew install ffmpeg`
  - Ubuntu/Debian: `sudo apt update && sudo apt install -y ffmpeg`

## Quick Start (Local Setup)

### 1. Create Virtual Environment

From the `ai-service/` directory, create and activate a Python virtual environment using Python 3.12:

```bash
cd ai-service
python3.12 -m venv .venv
source .venv/bin/activate
```

### 2. Install Dependencies

Install required runtime and development packages:

```bash
pip install -r requirements.txt
```

### 3. Environment Configuration

Copy the example environment configuration to `.env` if not already present:

```bash
cp .env.example .env
```

Configurable options in `.env`:
- `WHISPER_MODEL`: OpenAI Whisper model size (`tiny`, `base`, `small`, `medium`, `large`). Defaults to `tiny` for lightweight local development.

### 4. Run Development Server

Start the FastAPI application using Uvicorn:

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The service will be available at `http://localhost:8000`.

Interactive Swagger API documentation is available at `http://localhost:8000/docs`.

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Root endpoint displaying service metadata |
| GET | `/health` | Health check endpoint returning service status |
| POST | `/transcribe` | Upload an audio file to receive a Speech-to-Text transcript |

### Example Request (`POST /transcribe`)

```bash
curl -X POST "http://localhost:8000/transcribe" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/sample_audio.mp3"
```

### Example Response

```json
{
  "filename": "sample_audio.mp3",
  "text": "Welcome to the AI Meeting Knowledge Platform.",
  "language": "en",
  "segments": [
    {
      "id": 0,
      "start": 0.0,
      "end": 3.12,
      "text": "Welcome to the AI Meeting Knowledge Platform."
    }
  ]
}
```

## Running Automated Tests

Execute automated unit and integration tests with `pytest`:

```bash
pytest
```

*(Note: Unit tests use mocks for the Whisper model layer to run rapidly without downloading models or requiring heavy processing).*
