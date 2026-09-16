package com.aimeetingknowledge.platform.search.dto;

import com.aimeetingknowledge.platform.aiservice.dto.SearchReferenceDto;
import java.util.List;

public record SearchApiResponse(
        String answer,
        List<SearchReferenceDto> references
) {
}
