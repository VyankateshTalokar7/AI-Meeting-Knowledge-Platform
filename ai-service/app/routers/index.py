import logging
from fastapi import APIRouter, HTTPException, Depends

from app.schemas.indexing import IndexTranscriptRequest, IndexTranscriptResponse
from app.services.vector_store_service import VectorStoreService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["indexing"])


def get_vector_store_service() -> VectorStoreService:
    return VectorStoreService()


@router.post("/index-transcript", response_model=IndexTranscriptResponse)
def index_transcript(
    request: IndexTranscriptRequest,
    service: VectorStoreService = Depends(get_vector_store_service),
) -> IndexTranscriptResponse:
    try:
        return service.index_transcript(request)
    except Exception as exc:
        logger.error(f"Error during transcript vector indexing for meeting_id {request.meeting_id}: {exc}")
        raise HTTPException(
            status_code=500,
            detail="Failed to index transcript vectors."
        )
