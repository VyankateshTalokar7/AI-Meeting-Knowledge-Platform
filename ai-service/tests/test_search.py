import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.search import SearchRequest, SearchResponse, SearchReference
from app.services.vector_store_service import VectorStoreService


@pytest.fixture
def client():
    return TestClient(app)


def test_search_validation_blank_query(client):
    response = client.post("/search", json={"query": "", "meeting_ids": [1], "top_k": 5})
    assert response.status_code == 422


def test_search_validation_query_too_long(client):
    response = client.post("/search", json={"query": "A" * 1001, "meeting_ids": [1], "top_k": 5})
    assert response.status_code == 422


def test_search_validation_invalid_top_k(client):
    response = client.post("/search", json={"query": "Valid query", "meeting_ids": [1], "top_k": 0})
    assert response.status_code == 422

    response = client.post("/search", json={"query": "Valid query", "meeting_ids": [1], "top_k": 25})
    assert response.status_code == 422


import asyncio


def test_search_empty_allowed_meeting_ids():
    service = VectorStoreService()
    res = asyncio.run(service.perform_rag_search(
        query="What was decided?",
        allowed_meeting_ids=[],
        top_k=5
    ))
    assert res.answer == "No meeting knowledge is available because you have no meetings."
    assert res.references == []


@patch("app.services.vector_store_service.get_embedding_model")
@patch("app.services.vector_store_service.VectorStoreService._get_collection")
def test_search_similar_chunks_chroma_filtering_single_and_multiple_ids(mock_get_collection, mock_get_embedding):
    mock_collection = MagicMock()
    mock_get_collection.return_value = mock_collection

    mock_model = MagicMock()
    mock_model.encode.return_value = [[0.1] * 384]
    mock_get_embedding.return_value = mock_model

    mock_collection.query.return_value = {
        "documents": [["Retrieved text chunk"]],
        "metadatas": [[{
            "meeting_id": 10,
            "transcript_id": 20,
            "chunk_index": 0,
            "start_time": 1.0,
            "end_time": 5.0,
            "language": "en"
        }]],
        "ids": [["meeting_10_chunk_0"]],
        "distances": [[0.12]]
    }

    service = VectorStoreService()

    # Test single meeting ID
    chunks = service.search_similar_chunks(query="Test query", allowed_meeting_ids=[10], top_k=3)
    assert len(chunks) == 1
    assert chunks[0]["text"] == "Retrieved text chunk"
    assert chunks[0]["metadata"]["meeting_id"] == 10
    mock_collection.query.assert_called_with(
        query_embeddings=[[0.1] * 384],
        n_results=3,
        where={"meeting_id": 10}
    )

    # Test multiple meeting IDs using $in operator
    chunks = service.search_similar_chunks(query="Test query", allowed_meeting_ids=[10, 20], top_k=5)
    mock_collection.query.assert_called_with(
        query_embeddings=[[0.1] * 384],
        n_results=5,
        where={"meeting_id": {"$in": [10, 20]}}
    )


@patch("app.services.vector_store_service.VectorStoreService.search_similar_chunks")
def test_perform_rag_search_no_results(mock_search_similar):
    mock_search_similar.return_value = []
    service = VectorStoreService()

    res = asyncio.run(service.perform_rag_search(
        query="Unmatched query",
        allowed_meeting_ids=[1, 2],
        top_k=5
    ))
    assert res.answer == "The requested information was not found in the available meeting knowledge."
    assert res.references == []


@patch("app.services.vector_store_service.VectorStoreService.search_similar_chunks")
def test_perform_rag_search_successful_rag_and_references(mock_search_similar):
    mock_search_similar.return_value = [
        {
            "id": "meeting_1_chunk_0",
            "text": "The team decided to use Whisper for speech-to-text.",
            "metadata": {
                "meeting_id": 1,
                "transcript_id": 100,
                "chunk_index": 0,
                "start_time": 10.0,
                "end_time": 25.0,
                "start_segment_order": 0,
                "end_segment_order": 2,
                "language": "en"
            },
            "distance": 0.05
        }
    ]

    mock_llm_service = MagicMock()
    mock_llm_service.generate_rag_answer = AsyncMock(
        return_value="The team decided to use Whisper for the speech-to-text module."
    )

    service = VectorStoreService()
    res = asyncio.run(service.perform_rag_search(
        query="What decision was made about speech-to-text?",
        allowed_meeting_ids=[1],
        top_k=5,
        llm_service=mock_llm_service
    ))

    assert res.answer == "The team decided to use Whisper for the speech-to-text module."
    assert len(res.references) == 1
    ref = res.references[0]
    assert ref.meeting_id == 1
    assert ref.transcript_id == 100
    assert ref.chunk_index == 0
    assert ref.start_time == 10.0
    assert ref.end_time == 25.0

    # Verify RAG call prompt context contains retrieved text
    mock_llm_service.generate_rag_answer.assert_called_once()
    _, kwargs = mock_llm_service.generate_rag_answer.call_args
    assert "The team decided to use Whisper for speech-to-text." in kwargs["context"]



@patch("app.services.vector_store_service.VectorStoreService.perform_rag_search")
def test_search_endpoint_llm_failure_returns_500(mock_perform_rag, client):
    mock_perform_rag.side_effect = Exception("OpenRouter API error")

    payload = {
        "query": "Speech-to-text module?",
        "meeting_ids": [1],
        "top_k": 5
    }

    response = client.post("/search", json=payload)
    assert response.status_code == 500
    assert response.json()["detail"] == "Failed to perform semantic search and answer generation."
