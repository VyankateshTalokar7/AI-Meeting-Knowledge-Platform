package com.aimeetingknowledge.platform.aiservice.dto;

import java.util.List;

public record SearchAiServiceResponse(
        String answer,
        List<SearchReferenceDto> references
) {
}
