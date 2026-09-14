import logging
from fastapi import APIRouter, Depends, HTTPException, status
from app.config import get_settings, Settings
from app.schemas.analysis import AnalysisRequest, MeetingAnalysisSchema
from app.services.llm_service import LLMService, LLMError, get_llm_service

logger = logging.getLogger("ai-service.routers.analyze")
router = APIRouter(tags=["analysis"])


@router.post(
    "/analyze",
    status_code=status.HTTP_200_OK,
    response_model=MeetingAnalysisSchema,
    summary="Extract structured meeting knowledge (summary, topics, decisions, action items) using LLM",
)
async def analyze_meeting(
    request: AnalysisRequest,
    settings: Settings = Depends(get_settings),
    llm_service: LLMService = Depends(get_llm_service),
):
    text = (request.text or "").strip()

    # Empty transcript handling
    if not text:
        return MeetingAnalysisSchema(
            summary="",
            key_topics=[],
            decisions=[],
            action_items=[],
        )

    # Transcript length check
    if len(text) > settings.max_analysis_transcript_chars:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Transcript text exceeds maximum allowed limit of {settings.max_analysis_transcript_chars} characters for analysis.",
        )

    try:
        return await llm_service.analyze_meeting(text)
    except LLMError as exc:
        logger.error("Meeting analysis failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LLM meeting analysis failed.",
        )
    except Exception as exc:
        logger.error("Unexpected error during meeting analysis: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred while analyzing the meeting transcript.",
        )
