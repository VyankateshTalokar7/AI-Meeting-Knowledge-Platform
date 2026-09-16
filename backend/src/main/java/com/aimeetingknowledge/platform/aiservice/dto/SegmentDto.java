package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SegmentDto(
        @JsonProperty("segment_order") Integer segmentOrder,
        @JsonProperty("start_time") Double startTime,
        @JsonProperty("end_time") Double endTime,
        String text
) {
}
