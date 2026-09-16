import tempfile
import pytest
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.indexing import SegmentInput, IndexTranscriptRequest, IndexTranscriptResponse
from app.services.vector_store_service import chunk_segments, VectorStoreService


@pytest.fixture
def client():
    return TestClient(app)


def test_chunk_segments_empty_input():
    chunks = chunk_segments(
        meeting_id=1,
        transcript_id=10,
        language="en",
        segments=[],
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )
    assert chunks == []

    # Whitespace only
    empty_segs = [SegmentInput(segment_order=0, start_time=0.0, end_time=1.0, text="   ")]
    chunks = chunk_segments(
        meeting_id=1,
        transcript_id=10,
        language="en",
        segments=empty_segs,
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )
    assert chunks == []


def test_chunk_segments_normal_chunking_and_target_max_behavior():
    # Create 4 segments of 250 characters each
    seg0 = SegmentInput(segment_order=0, start_time=0.0, end_time=10.0, text="A" * 250)
    seg1 = SegmentInput(segment_order=1, start_time=10.0, end_time=20.0, text="B" * 250)
    seg2 = SegmentInput(segment_order=2, start_time=20.0, end_time=30.0, text="C" * 250)
    seg3 = SegmentInput(segment_order=3, start_time=30.0, end_time=40.0, text="D" * 250)

    # seg0 + seg1 = 501 chars (<700, <=1000)
    # seg0 + seg1 + seg2 = 752 chars (candidate_len <= 1000, but curr_len was 501 < 700 before adding seg2, so seg2 is added -> curr_len becomes 752 >= 700)
    # Next segment candidate seg3 would make total 752 + 1 + 250 = 1003 (>1000 max size), so seg3 breaks.
    # Chunk 0 gets seg0, seg1, seg2.

    chunks = chunk_segments(
        meeting_id=42,
        transcript_id=101,
        language="en",
        segments=[seg0, seg1, seg2, seg3],
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )

    assert len(chunks) >= 1
    chunk0 = chunks[0]
    assert chunk0["id"] == "meeting_42_chunk_0"
    assert chunk0["metadata"]["meeting_id"] == 42
    assert chunk0["metadata"]["transcript_id"] == 101
    assert chunk0["metadata"]["chunk_index"] == 0
    assert chunk0["metadata"]["start_time"] == 0.0
    assert chunk0["metadata"]["end_time"] == 30.0
    assert chunk0["metadata"]["start_segment_order"] == 0
    assert chunk0["metadata"]["end_segment_order"] == 2
    assert chunk0["metadata"]["language"] == "en"
    assert len(chunk0["text"]) <= 1000


def test_chunk_segments_no_segment_splitting_and_oversized_segment():
    # One segment of 1200 characters (exceeds max 1000)
    oversized_text = "X" * 1200
    seg0 = SegmentInput(segment_order=0, start_time=0.0, end_time=15.0, text=oversized_text)
    seg1 = SegmentInput(segment_order=1, start_time=15.0, end_time=25.0, text="Short segment.")

    chunks = chunk_segments(
        meeting_id=5,
        transcript_id=12,
        language="en",
        segments=[seg0, seg1],
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )

    assert len(chunks) == 2
    # Oversized segment is not split
    assert chunks[0]["text"] == oversized_text
    assert chunks[0]["metadata"]["start_segment_order"] == 0
    assert chunks[0]["metadata"]["end_segment_order"] == 0

    assert chunks[1]["text"] == "Short segment."
    assert chunks[1]["metadata"]["start_segment_order"] == 1
    assert chunks[1]["metadata"]["end_segment_order"] == 1


def test_chunk_segments_overlap_behavior():
    # Segments:
    # seg0: 500 chars
    # seg1: 250 chars -> seg0 + seg1 = 751 chars (Chunk 0: seg0, seg1).
    # Overlap check for Chunk 0: seg1 is 250 chars > 150 overlap_size -> overlap cannot fit seg1.
    # So next chunk starts at seg2.

    # Now test where a small segment fits in overlap:
    # seg0: 600 chars
    # seg1: 100 chars -> Chunk 0: seg0 + seg1 = 701 chars.
    # Overlap check: seg1 (100 chars) <= 150! So overlap includes seg1.
    # Chunk 1 starts at seg1.
    seg0 = SegmentInput(segment_order=0, start_time=0.0, end_time=5.0, text="A" * 600)
    seg1 = SegmentInput(segment_order=1, start_time=5.0, end_time=10.0, text="B" * 100)
    seg2 = SegmentInput(segment_order=2, start_time=10.0, end_time=15.0, text="C" * 300)

    chunks = chunk_segments(
        meeting_id=7,
        transcript_id=8,
        language="en",
        segments=[seg0, seg1, seg2],
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )

    assert len(chunks) == 2
    assert chunks[0]["metadata"]["start_segment_order"] == 0
    assert chunks[0]["metadata"]["end_segment_order"] == 1
    # Chunk 1 should overlap seg1
    assert chunks[1]["metadata"]["start_segment_order"] == 1
    assert chunks[1]["metadata"]["end_segment_order"] == 2


def test_deterministic_ids_timestamps_metadata():
    seg0 = SegmentInput(segment_order=0, start_time=1.5, end_time=4.2, text="First segment")
    seg1 = SegmentInput(segment_order=1, start_time=4.2, end_time=9.8, text="Second segment")

    chunks = chunk_segments(
        meeting_id=123,
        transcript_id=456,
        language="fr",
        segments=[seg0, seg1],
        target_size=700,
        max_size=1000,
        overlap_size=150,
    )

    assert len(chunks) == 1
    chunk = chunks[0]
    assert chunk["id"] == "meeting_123_chunk_0"
    meta = chunk["metadata"]
    assert meta["meeting_id"] == 123
    assert meta["transcript_id"] == 456
    assert meta["chunk_index"] == 0
    assert meta["start_time"] == 1.5
    assert meta["end_time"] == 9.8
    assert meta["start_segment_order"] == 0
    assert meta["end_segment_order"] == 1
    assert meta["language"] == "fr"


def test_index_transcript_empty_segments_noop(client):
    req_payload = {
        "meeting_id": 99,
        "transcript_id": 100,
        "language": "en",
        "segments": []
    }
    response = client.post("/index-transcript", json=req_payload)
    assert response.status_code == 200
    res_data = response.json()
    assert res_data["status"] == "skipped_empty"
    assert res_data["meeting_id"] == 99
    assert res_data["transcript_id"] == 100
    assert res_data["chunks_indexed"] == 0


@patch("app.services.vector_store_service.get_embedding_model")
@patch("app.services.vector_store_service.VectorStoreService._get_collection")
def test_meeting_scoped_replacement_and_batching(mock_get_collection, mock_get_embedding_model, client):
    mock_collection = MagicMock()
    mock_get_collection.return_value = mock_collection

    mock_model = MagicMock()
    # Mock encode to return dummy embeddings
    mock_model.encode.side_effect = lambda texts, batch_size, show_progress_bar: [[0.1] * 384 for _ in texts]
    mock_get_embedding_model.return_value = mock_model

    # Create 35 segments to test batching at 32
    segments = [
        {"segment_order": i, "start_time": float(i), "end_time": float(i + 1), "text": f"Segment text {i} " + "A" * 700}
        for i in range(35)
    ]

    req_payload = {
        "meeting_id": 88,
        "transcript_id": 200,
        "language": "en",
        "segments": segments,
    }

    response = client.post("/index-transcript", json=req_payload)
    assert response.status_code == 200
    res_data = response.json()
    assert res_data["status"] == "indexed"
    assert res_data["meeting_id"] == 88
    assert res_data["transcript_id"] == 200
    assert res_data["chunks_indexed"] == 35

    # Verify meeting-scoped replacement deletion occurred first
    mock_collection.delete.assert_called_once_with(where={"meeting_id": 88})

    # Verify batching: 35 chunks should result in 2 upsert calls (32 + 3)
    assert mock_collection.upsert.call_count == 2


@patch("app.services.vector_store_service.VectorStoreService.index_transcript")
def test_indexing_failure_returns_500(mock_index, client):
    mock_index.side_effect = Exception("Chroma connection error")

    req_payload = {
        "meeting_id": 77,
        "transcript_id": 300,
        "language": "en",
        "segments": [{"segment_order": 0, "start_time": 0.0, "end_time": 1.0, "text": "Test"}]
    }

    response = client.post("/index-transcript", json=req_payload)
    assert response.status_code == 500
    assert response.json()["detail"] == "Failed to index transcript vectors."
