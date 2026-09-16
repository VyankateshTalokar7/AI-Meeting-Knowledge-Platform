import logging
from fastapi import APIRouter, HTTPException, Depends

from app.schemas.search import SearchRequest, SearchResponse
from app.services.vector_store_service import VectorStoreService
from app.services.llm_service import LLMService, get_llm_service

logger = logging.getLogger(__name__)

router = APIRouter(tags=["search"])


def get_vector_store_service() -> VectorStoreService:
    return VectorStoreService()


@router.post("/search", response_model=SearchResponse)
async def search_meetings(
    request: SearchRequest,
    vector_service: VectorStoreService = Depends(get_vector_store_service),
    llm_service: LLMService = Depends(get_llm_service),
) -> SearchResponse:
    try:
        return await vector_service.perform_rag_search(
            query=request.query,
            allowed_meeting_ids=request.meeting_ids,
            top_k=request.top_k,
            llm_service=llm_service,
        )
    except Exception as exc:
        logger.error(f"Error during semantic search and answer generation: {exc}")
        raise HTTPException(
            status_code=500,
            detail="Failed to perform semantic search and answer generation."
        )
