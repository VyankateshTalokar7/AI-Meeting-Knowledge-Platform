package com.aimeetingknowledge.platform.aiservice.dto;

public record SegmentResponse(
        Integer id,
        Double start,
        Double end,
        String text
) {
}
