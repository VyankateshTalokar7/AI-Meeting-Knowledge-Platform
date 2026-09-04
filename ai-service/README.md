# AI Service — Speech-to-Text & Processing Microservice

FastAPI microservice foundation for handling audio transcription and future AI knowledge processing for the AI Meeting Knowledge Platform.

## Prerequisites

- Python 3.10+ (Tested on Python 3.13)

## Quick Start (Local Setup)

### 1. Create Virtual Environment

From the `ai-service/` directory, create and activate a Python virtual environment:

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
```

### 2. Install Dependencies

Install required runtime and development packages:

```bash
pip install -r requirements.txt
```

### 3. Environment Configuration

Copy the example environment configuration to `.env`:

```bash
cp .env.example .env
```

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

## Running Tests

Execute automated unit tests with `pytest`:

```bash
pytest
```
