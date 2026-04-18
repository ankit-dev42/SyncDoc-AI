package com.syncdoc.collaboration.ai.service;

import com.syncdoc.collaboration.ai.model.GeneratedDocumentation;
import com.syncdoc.collaboration.ai.parser.GeneratedDocumentationParser;
import com.syncdoc.collaboration.ai.repository.GeneratedDocumentationRepository;
import com.syncdoc.collaboration.config.BusinessValidationProperties;
import com.syncdoc.collaboration.exception.BusinessValidationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AIProcessingService {

    private final GeneratedDocumentationRepository generatedDocumentationRepository;
    private final GeneratedDocumentationParser generatedDocumentationParser;
    private final AIExtractionClient aiExtractionClient;

    public AIProcessingService(
        GeneratedDocumentationRepository generatedDocumentationRepository,
        GeneratedDocumentationParser generatedDocumentationParser,
        AIExtractionClient aiExtractionClient
    ) {
        this.generatedDocumentationRepository = generatedDocumentationRepository;
        this.generatedDocumentationParser = generatedDocumentationParser;
        this.aiExtractionClient = aiExtractionClient;
    }

    public GeneratedDocumentation processExtraction(String userId, String sourceContentId, String sourceContent) {
        GeneratedDocumentation documentation = new GeneratedDocumentation();
        documentation.setUserId(userId);
        documentation.setSourceContentId(sourceContentId);
        documentation.setSourceContent(sourceContent);

        try {
            String markdownResponse = aiExtractionClient.extractDocumentation(sourceContent);
            GeneratedDocumentationParser.ParsedSections sections = generatedDocumentationParser.parse(markdownResponse);

            documentation.setKeyChanges(sections.keyChanges());
            documentation.setActionItems(sections.actionItems());
            documentation.setStatus(GeneratedDocumentation.ProcessingStatus.COMPLETED);
            documentation.setQualityScore(calculateQualityScore(sections));
            documentation.setCompletedAt(Instant.now());
        } catch (RuntimeException ex) {
            documentation.setStatus(GeneratedDocumentation.ProcessingStatus.FAILED);
            documentation.setProcessingError(ex.getMessage());
            documentation.setCompletedAt(Instant.now());
        }

        return generatedDocumentationRepository.save(documentation);
    }

    public GeneratedDocumentation getDocumentation(String docId) {
        return generatedDocumentationRepository.findById(docId)
            .orElseThrow(() -> new BusinessValidationException(
                404,
                "DOCUMENTATION_NOT_FOUND",
                "Generated documentation not found."
            ));
    }

    public GeneratedDocumentation getCompletedDocumentation(String docId) {
        GeneratedDocumentation documentation = getDocumentation(docId);
        if (documentation.getStatus() != GeneratedDocumentation.ProcessingStatus.COMPLETED) {
            throw new BusinessValidationException(
                409,
                "EXTRACTION_NOT_READY",
                "Extraction result is not ready."
            );
        }
        return documentation;
    }

    private double calculateQualityScore(GeneratedDocumentationParser.ParsedSections sections) {
        int populatedSections = 0;
        if (!sections.keyChanges().isBlank()) {
            populatedSections++;
        }
        if (!sections.actionItems().isBlank()) {
            populatedSections++;
        }
        return populatedSections / 2.0d;
    }

    public interface AIExtractionClient {
        String extractDocumentation(String sourceContent);
    }
}

@Component
class DefaultAIExtractionClient implements AIProcessingService.AIExtractionClient {

    private final BusinessValidationProperties businessValidationProperties;

    DefaultAIExtractionClient(BusinessValidationProperties businessValidationProperties) {
        this.businessValidationProperties = businessValidationProperties;
    }

    @Override
    public String extractDocumentation(String sourceContent) {
        String configuredModel = businessValidationProperties.getOpenai().getModel();
        if (sourceContent == null || sourceContent.isBlank()) {
            throw new IllegalStateException("Source content is required for extraction");
        }

        // Placeholder seam until a real OpenAI client is wired in Phase 8 quality gates.
        return "## Key Changes\n" + sourceContent + "\n\n## Action Items\nReview and refine output using model " + configuredModel;
    }
}