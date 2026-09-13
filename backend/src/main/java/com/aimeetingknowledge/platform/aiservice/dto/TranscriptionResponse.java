package com.aimeetingknowledge.platform.aiservice.dto;

import java.util.List;

public record TranscriptionResponse(
        String filename,
        String text,
        String language,
        List<SegmentResponse> segments
) {
}
