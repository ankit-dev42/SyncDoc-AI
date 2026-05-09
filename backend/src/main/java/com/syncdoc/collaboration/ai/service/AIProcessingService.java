package com.syncdoc.collaboration.ai.service;

import com.syncdoc.collaboration.ai.model.GeneratedDocumentation;
import com.syncdoc.collaboration.ai.parser.GeneratedDocumentationParser;
import com.syncdoc.collaboration.ai.repository.GeneratedDocumentationRepository;
import com.syncdoc.collaboration.config.BusinessValidationProperties;
import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.observability.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AIProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AIProcessingService.class);

    private final GeneratedDocumentationRepository generatedDocumentationRepository;
    private final GeneratedDocumentationParser generatedDocumentationParser;
    private final AIExtractionClient aiExtractionClient;
    private final AuditLogger auditLogger;

    public AIProcessingService(
        GeneratedDocumentationRepository generatedDocumentationRepository,
        GeneratedDocumentationParser generatedDocumentationParser,
        AIExtractionClient aiExtractionClient,
        AuditLogger auditLogger
    ) {
        this.generatedDocumentationRepository = generatedDocumentationRepository;
        this.generatedDocumentationParser = generatedDocumentationParser;
        this.aiExtractionClient = aiExtractionClient;
        this.auditLogger = auditLogger;
    }

    @Async("aiExtractionExecutor")
    public GeneratedDocumentation processExtraction(String userId, String sourceContentId, String sourceContent) {
        long startTime = System.currentTimeMillis();
        GeneratedDocumentation documentation = new GeneratedDocumentation();
        documentation.setUserId(userId);
        documentation.setSourceContentId(sourceContentId);
        documentation.setSourceContent(sourceContent);
        documentation.setStatus(GeneratedDocumentation.ProcessingStatus.PROCESSING);
        generatedDocumentationRepository.save(documentation);

        try {
            String markdownResponse = aiExtractionClient.extractDocumentation(sourceContent);
            GeneratedDocumentationParser.ParsedSections sections = generatedDocumentationParser.parse(markdownResponse);

            documentation.setKeyChanges(sections.keyChanges());
            documentation.setActionItems(sections.actionItems());
            documentation.setStatus(GeneratedDocumentation.ProcessingStatus.COMPLETED);
            documentation.setQualityScore(calculateQualityScore(sections, sourceContent));
            documentation.setCompletedAt(Instant.now());
            GeneratedDocumentation saved = generatedDocumentationRepository.save(documentation);
            long durationMs = System.currentTimeMillis() - startTime;
            double score = saved.getQualityScore() != null ? saved.getQualityScore() : 0.0;
            auditLogger.aiExtractionCompleted(saved.getId(), score, durationMs, userId, "-");
            return saved;
        } catch (Exception ex) {
            log.error("AI extraction failed for sourceContentId={}: {}", sourceContentId, ex.getMessage(), ex);
            documentation.setStatus(GeneratedDocumentation.ProcessingStatus.FAILED);
            documentation.setProcessingError(ex.getMessage());
            documentation.setCompletedAt(Instant.now());
            GeneratedDocumentation saved = generatedDocumentationRepository.save(documentation);
            auditLogger.aiExtractionFailed(saved.getId(), ex.getClass().getSimpleName(), userId, "-");
            return saved;
        }
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

    private double calculateQualityScore(GeneratedDocumentationParser.ParsedSections sections, String sourceContent) {
        int sectionsFound = 0;
        if (!sections.keyChanges().isBlank()) {
            sectionsFound++;
        }
        if (!sections.actionItems().isBlank()) {
            sectionsFound++;
        }
        int contentLength = sourceContent != null ? sourceContent.length() : 0;
        return (sectionsFound / 2.0) * Math.min(1.0, contentLength / 500.0);
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
