package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingAnalysisResponse(
        String summary,
        @JsonProperty("key_topics") List<String> keyTopics,
        List<String> decisions,
        @JsonProperty("action_items") List<ActionItemDto> actionItems
) {
}
