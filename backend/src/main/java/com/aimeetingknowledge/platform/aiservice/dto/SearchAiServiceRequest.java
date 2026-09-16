package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SearchAiServiceRequest(
        String query,
        @JsonProperty("meeting_ids") List<Long> meetingIds,
        @JsonProperty("top_k") Integer topK
) {
}
