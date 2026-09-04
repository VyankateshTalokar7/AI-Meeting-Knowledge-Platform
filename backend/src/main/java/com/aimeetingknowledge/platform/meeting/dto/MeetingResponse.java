package com.aimeetingknowledge.platform.meeting.dto;

import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;

import java.time.Instant;

public record MeetingResponse(
        Long id,
        String title,
        String description,
        Instant meetingDate,
        MeetingStatus status,
        Instant createdAt
) {
    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getMeetingDate(),
                meeting.getStatus(),
                meeting.getCreatedAt()
        );
    }
}
