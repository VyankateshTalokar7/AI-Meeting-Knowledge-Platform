package com.aimeetingknowledge.platform.meeting.audio.dto;

import com.aimeetingknowledge.platform.meeting.audio.MeetingAudio;

import java.time.Instant;

public record MeetingAudioResponse(
        Long id,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt
) {
    public static MeetingAudioResponse from(MeetingAudio audio) {
        return new MeetingAudioResponse(
                audio.getId(), audio.getOriginalFilename(), audio.getContentType(), audio.getFileSize(), audio.getCreatedAt()
        );
    }
}
