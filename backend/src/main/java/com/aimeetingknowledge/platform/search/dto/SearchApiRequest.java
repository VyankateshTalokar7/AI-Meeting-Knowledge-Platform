package com.aimeetingknowledge.platform.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchApiRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(max = 1000, message = "Query maximum length is 1000 characters")
        String query,

        @Min(value = 1, message = "top_k must be at least 1")
        @Max(value = 20, message = "top_k cannot exceed 20")
        Integer topK
) {
}
