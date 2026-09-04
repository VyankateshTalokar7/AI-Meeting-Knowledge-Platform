import os
import pathlib
import tempfile
import logging
from fastapi import APIRouter, File, HTTPException, UploadFile, status
from app.config import get_settings
from app.services.transcription import transcription_service, TranscriptionError

logger = logging.getLogger("ai-service.routers.transcribe")
router = APIRouter(tags=["transcription"])


@router.post(
    "/transcribe",
    status_code=status.HTTP_200_OK,
    summary="Transcribe an audio file using Speech-to-Text",
    response_description="Transcription result with full text and timing segments",
)
async def transcribe_audio(file: UploadFile = File(...)):
    settings = get_settings()

    if not file or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No file was uploaded.",
        )

    content_type = (file.content_type or "").lower().strip()
    if content_type not in settings.allowed_content_types:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file type '{file.content_type}'. Allowed content types: {', '.join(settings.allowed_content_types)}",
        )

    # Determine safe file suffix
    ext = pathlib.Path(file.filename).suffix.lower()
    if not ext or len(ext) > 10:
        ext = ".audio"

    tmp_path = None
    try:
        # Create temporary file safely
        with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp_file:
            tmp_path = tmp_file.name
            size = 0
            chunk_size = 1024 * 1024  # 1 MB chunk
            while chunk := await file.read(chunk_size):
                size += len(chunk)
                if size > settings.max_upload_size_bytes:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=f"File size exceeds maximum limit of {settings.max_upload_size_bytes} bytes.",
                    )
                tmp_file.write(chunk)

        if size == 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Uploaded file is empty.",
            )

        # Transcribe audio using service layer
        result = transcription_service.transcribe_audio(tmp_path)
        return {
            "filename": file.filename,
            "text": result["text"],
            "language": result["language"],
            "segments": result["segments"],
        }

    except HTTPException:
        raise
    except TranscriptionError as exc:
        logger.error("Transcription service error: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Audio transcription failed.",
        )
    except Exception as exc:
        logger.error("Unexpected error during file upload/transcription: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred while processing the audio file.",
        )
    finally:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
            except Exception as cleanup_exc:
                logger.warning("Failed to delete temporary file: %s", cleanup_exc)
        try:
            await file.close()
        except Exception as close_exc:
            logger.warning("Failed to close upload file: %s", close_exc)

