from pydantic import BaseModel, Field


class SegmentInput(BaseModel):
    segment_order: int = Field(..., ge=0, description="Index order of the segment in the transcript")
    start_time: float = Field(..., ge=0.0, description="Start timestamp in seconds")
    end_time: float = Field(..., ge=0.0, description="End timestamp in seconds")
    text: str = Field(..., description="Transcript segment text")


class IndexTranscriptRequest(BaseModel):
    meeting_id: int = Field(..., gt=0, description="Database ID of the meeting")
    transcript_id: int = Field(..., gt=0, description="Database ID of the transcript")
    language: str = Field(default="en", description="Language code of the transcript")
    segments: list[SegmentInput] = Field(default_factory=list, description="Timestamped segments of the transcript")


class IndexTranscriptResponse(BaseModel):
    status: str = Field(..., description="Status of the indexing operation")
    meeting_id: int = Field(..., description="ID of the processed meeting")
    transcript_id: int = Field(..., description="ID of the processed transcript")
    chunks_indexed: int = Field(..., description="Number of vector chunks indexed")
