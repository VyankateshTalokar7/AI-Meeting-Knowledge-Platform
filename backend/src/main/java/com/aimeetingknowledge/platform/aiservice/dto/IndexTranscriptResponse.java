package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexTranscriptResponse(
        String status,
        @JsonProperty("meeting_id") Long meetingId,
        @JsonProperty("transcript_id") Long transcriptId,
        @JsonProperty("chunks_indexed") Integer chunksIndexed
) {
}
