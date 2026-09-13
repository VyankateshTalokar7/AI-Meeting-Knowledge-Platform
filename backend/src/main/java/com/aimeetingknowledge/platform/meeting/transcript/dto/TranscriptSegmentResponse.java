package com.aimeetingknowledge.platform.meeting.transcript.dto;

import com.aimeetingknowledge.platform.meeting.transcript.TranscriptSegment;

public record TranscriptSegmentResponse(
        Integer segmentOrder,
        Double startTime,
        Double endTime,
        String text
) {
    public static TranscriptSegmentResponse from(TranscriptSegment segment) {
        return new TranscriptSegmentResponse(
                segment.getSegmentOrder(),
                segment.getStartTime(),
                segment.getEndTime(),
                segment.getText()
        );
    }
}
