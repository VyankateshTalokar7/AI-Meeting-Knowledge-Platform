package com.aimeetingknowledge.platform.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateMeetingRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must not exceed 200 characters.")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description,

        @NotNull(message = "Meeting date is required.")
        Instant meetingDate
) {
}
