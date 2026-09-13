package com.aimeetingknowledge.platform.meeting.audio.event;

public record TranscriptionRequestedEvent(
        Long meetingId,
        String storagePath
) {
}
