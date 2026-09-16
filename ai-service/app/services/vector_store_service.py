import os
import logging
from typing import Optional, List, Dict, Any

from app.config import get_settings, Settings
from app.schemas.indexing import SegmentInput, IndexTranscriptRequest, IndexTranscriptResponse

logger = logging.getLogger(__name__)

# Cached model instance
_model_instance = None


def get_embedding_model(model_name: str):
    global _model_instance
    if _model_instance is None:
        logger.info(f"Loading embedding model: {model_name}")
        from sentence_transformers import SentenceTransformer
        _model_instance = SentenceTransformer(model_name)
    return _model_instance


def chunk_segments(
    meeting_id: int,
    transcript_id: int,
    language: str,
    segments: List[SegmentInput],
    target_size: int = 700,
    max_size: int = 1000,
    overlap_size: int = 150,
) -> List[Dict[str, Any]]:
    valid_segments = [s for s in segments if s.text and s.text.strip()]
    if not valid_segments:
        return []

    chunks: List[Dict[str, Any]] = []
    n = len(valid_segments)
    i = 0
    chunk_index = 0

    while i < n:
        first_seg = valid_segments[i]
        first_text = first_seg.text.strip()

        # Handle single segment that exceeds or equals max_size
        if len(first_text) >= max_size:
            chunks.append({
                "id": f"meeting_{meeting_id}_chunk_{chunk_index}",
                "text": first_text,
                "metadata": {
                    "meeting_id": meeting_id,
                    "transcript_id": transcript_id,
                    "chunk_index": chunk_index,
                    "start_time": float(first_seg.start_time),
                    "end_time": float(first_seg.end_time),
                    "start_segment_order": int(first_seg.segment_order),
                    "end_segment_order": int(first_seg.segment_order),
                    "language": language,
                }
            })
            chunk_index += 1
            i += 1
            continue

        # Accumulate segments for normal chunk
        curr_segs = [first_seg]
        curr_len = len(first_text)
        j = i + 1

        while j < n:
            next_text = valid_segments[j].text.strip()
            candidate_len = curr_len + 1 + len(next_text)

            if candidate_len > max_size:
                break
            if curr_len >= target_size:
                break

            curr_segs.append(valid_segments[j])
            curr_len = candidate_len
            j += 1

        chunk_text = " ".join(s.text.strip() for s in curr_segs)
        chunks.append({
            "id": f"meeting_{meeting_id}_chunk_{chunk_index}",
            "text": chunk_text,
            "metadata": {
                "meeting_id": meeting_id,
                "transcript_id": transcript_id,
                "chunk_index": chunk_index,
                "start_time": float(curr_segs[0].start_time),
                "end_time": float(curr_segs[-1].end_time),
                "start_segment_order": int(curr_segs[0].segment_order),
                "end_segment_order": int(curr_segs[-1].segment_order),
                "language": language,
            }
        })
        chunk_index += 1

        if j >= n:
            break

        # Calculate overlap starting segment from completed previous segments
        overlap_accum_len = 0
        overlap_start_idx = j

        for k in range(j - 1, i, -1):
            seg_len = len(valid_segments[k].text.strip())
            added_len = seg_len if overlap_accum_len == 0 else (overlap_accum_len + 1 + seg_len)
            if added_len <= overlap_size:
                overlap_accum_len = added_len
                overlap_start_idx = k
            else:
                break

        next_i = overlap_start_idx if overlap_start_idx > i else j
        i = next_i

    return chunks


class VectorStoreService:
    def __init__(self, settings: Optional[Settings] = None, chroma_client=None):
        self.settings = settings or get_settings()
        self._chroma_client = chroma_client

    def _get_client(self):
        if self._chroma_client is None:
            import chromadb
            persist_dir = os.path.abspath(self.settings.chroma_persist_directory)
            os.makedirs(persist_dir, exist_ok=True)
            self._chroma_client = chromadb.PersistentClient(path=persist_dir)
        return self._chroma_client

    def _get_collection(self):
        client = self._get_client()
        return client.get_or_create_collection(name=self.settings.chroma_collection_name)

    def index_transcript(self, request: IndexTranscriptRequest) -> IndexTranscriptResponse:
        valid_segments = [s for s in request.segments if s.text and s.text.strip()]
        if not valid_segments:
            logger.info(f"Empty segments for meeting_id {request.meeting_id}, skipping vector index update.")
            return IndexTranscriptResponse(
                status="skipped_empty",
                meeting_id=request.meeting_id,
                transcript_id=request.transcript_id,
                chunks_indexed=0,
            )

        chunks = chunk_segments(
            meeting_id=request.meeting_id,
            transcript_id=request.transcript_id,
            language=request.language,
            segments=request.segments,
            target_size=self.settings.chunk_target_size_chars,
            max_size=self.settings.chunk_max_size_chars,
            overlap_size=self.settings.chunk_overlap_chars,
        )

        if not chunks:
            return IndexTranscriptResponse(
                status="skipped_empty",
                meeting_id=request.meeting_id,
                transcript_id=request.transcript_id,
                chunks_indexed=0,
            )

        collection = self._get_collection()

        # Meeting-scoped replacement: delete existing vectors for this meeting
        try:
            collection.delete(where={"meeting_id": request.meeting_id})
        except Exception as exc:
            logger.warning(f"Error during existing vector deletion for meeting_id {request.meeting_id}: {exc}")

        # Embed and upsert in batches of 32
        model = get_embedding_model(self.settings.embedding_model_name)
        batch_size = 32

        for b in range(0, len(chunks), batch_size):
            batch = chunks[b : b + batch_size]
            batch_ids = [c["id"] for c in batch]
            batch_texts = [c["text"] for c in batch]
            batch_metadatas = [c["metadata"] for c in batch]

            embeddings = model.encode(batch_texts, batch_size=len(batch_texts), show_progress_bar=False)
            if hasattr(embeddings, "tolist"):
                embeddings = embeddings.tolist()


            collection.upsert(
                ids=batch_ids,
                documents=batch_texts,
                metadatas=batch_metadatas,
                embeddings=embeddings,
            )

        return IndexTranscriptResponse(
            status="indexed",
            meeting_id=request.meeting_id,
            transcript_id=request.transcript_id,
            chunks_indexed=len(chunks),
        )
