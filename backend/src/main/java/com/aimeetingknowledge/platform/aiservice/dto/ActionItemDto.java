package com.aimeetingknowledge.platform.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActionItemDto(
        String task,
        String assignee,
        @JsonProperty("due_date") String dueDate
) {
}
