from fastapi import FastAPI
from app.config import get_settings
from app.routers import analyze, index, search, transcribe

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    description="Speech-to-Text and AI Processing Microservice for AI Meeting Knowledge Platform",
    version="0.1.0",
)

app.include_router(transcribe.router)
app.include_router(analyze.router)
app.include_router(index.router)
app.include_router(search.router)




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
