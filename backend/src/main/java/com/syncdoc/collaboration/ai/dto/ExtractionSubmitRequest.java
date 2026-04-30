package com.syncdoc.collaboration.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractionSubmitRequest(
    @NotBlank @Size(max = 100000) String sourceContent,
    @NotBlank @Size(max = 50) String sourceContentId
) {
}
