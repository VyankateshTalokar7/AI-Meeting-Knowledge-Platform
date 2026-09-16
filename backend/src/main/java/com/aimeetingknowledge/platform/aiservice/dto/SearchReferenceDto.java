package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchReferenceDto(
        @JsonProperty("meeting_id") Long meetingId,
        @JsonProperty("transcript_id") Long transcriptId,
        @JsonProperty("chunk_index") Integer chunkIndex,
        @JsonProperty("start_time") Double startTime,
        @JsonProperty("end_time") Double endTime
) {
}
