package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IndexTranscriptRequest(
        @JsonProperty("meeting_id") Long meetingId,
        @JsonProperty("transcript_id") Long transcriptId,
        String language,
        List<SegmentDto> segments
) {
}
