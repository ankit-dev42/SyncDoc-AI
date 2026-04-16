package com.syncdoc.collaboration.ai.controller;

import com.syncdoc.collaboration.ai.model.GeneratedDocumentation;
import com.syncdoc.collaboration.ai.service.AIProcessingService;
import com.syncdoc.collaboration.common.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("isAuthenticated()")
public class AIExtractionController {

    private final AIProcessingService aiProcessingService;

    public AIExtractionController(AIProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @GetMapping("/extract-status/{docId}")
    public ResponseEntity<ApiResponse<ExtractionStatusResponse>> getExtractionStatus(
        @PathVariable @NotBlank String docId
    ) {
        GeneratedDocumentation documentation = aiProcessingService.getDocumentation(docId);
        ExtractionStatusResponse response = new ExtractionStatusResponse(
            documentation.getId(),
            documentation.getStatus().name()
        );
        return ResponseEntity.ok(ApiResponse.success("Extraction status retrieved", response));
    }

    @GetMapping("/extract-result/{docId}")
    public ResponseEntity<ApiResponse<ExtractionResultResponse>> getExtractionResult(
        @PathVariable @NotBlank String docId
    ) {
        GeneratedDocumentation documentation = aiProcessingService.getCompletedDocumentation(docId);
        ExtractionResultResponse response = new ExtractionResultResponse(
            documentation.getId(),
            documentation.getStatus().name(),
            documentation.getKeyChanges(),
            documentation.getActionItems(),
            documentation.getQualityScore()
        );
        return ResponseEntity.ok(ApiResponse.success("Extraction result retrieved", response));
    }

    public record ExtractionStatusResponse(
        String docId,
        String status
    ) {
    }

    public record ExtractionResultResponse(
        String docId,
        String status,
        String keyChanges,
        String actionItems,
        Double qualityScore
    ) {
    }
}