from fastapi import FastAPI
from app.config import get_settings

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    description="Speech-to-Text and AI Processing Microservice for AI Meeting Knowledge Platform",
    version="0.1.0",
)


@app.get("/")
def root():
    return {
        "message": "AI Meeting Knowledge Platform - AI Service",
        "version": "0.1.0",
        "docs_url": "/docs",
    }


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": settings.app_name,
        "environment": settings.environment,
    }
