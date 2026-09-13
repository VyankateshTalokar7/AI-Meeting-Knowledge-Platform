package com.aimeetingknowledge.platform.meeting.transcript.dto;

import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscript;

import java.time.Instant;
import java.util.List;

public record MeetingTranscriptResponse(
        Long id,
        Long meetingId,
        String fullText,
        String language,
        List<TranscriptSegmentResponse> segments,
        Instant createdAt
) {
    public static MeetingTranscriptResponse from(MeetingTranscript transcript) {
        List<TranscriptSegmentResponse> segmentResponses = transcript.getSegments().stream()
                .map(TranscriptSegmentResponse::from)
                .toList();

        return new MeetingTranscriptResponse(
                transcript.getId(),
                transcript.getMeeting().getId(),
                transcript.getFullText(),
                transcript.getLanguage(),
                segmentResponses,
                transcript.getCreatedAt()
        );
    }
}
