package com.syncdoc.collaboration.ai.unit;

import com.syncdoc.collaboration.ai.model.GeneratedDocumentation;
import com.syncdoc.collaboration.ai.parser.GeneratedDocumentationParser;
import com.syncdoc.collaboration.ai.repository.GeneratedDocumentationRepository;
import com.syncdoc.collaboration.ai.service.AIProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIProcessingServiceTest {

    @Mock
    private GeneratedDocumentationRepository generatedDocumentationRepository;

    @Mock
    private GeneratedDocumentationParser generatedDocumentationParser;

    @Mock
    private AIProcessingService.AIExtractionClient aiExtractionClient;

    @InjectMocks
    private AIProcessingService aiProcessingService;

    @Test
    void shouldPersistStructuredResultExactlyOnceOnSuccessfulProcessing() {
        when(aiExtractionClient.extractDocumentation("source body"))
            .thenReturn("## Key Changes\n- Added OAuth login\n## Action Items\n- Update docs");
        when(generatedDocumentationParser.parse(any()))
            .thenReturn(new GeneratedDocumentationParser.ParsedSections(
                "- Added OAuth login",
                "- Update docs"
            ));
        when(generatedDocumentationRepository.save(any(GeneratedDocumentation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        GeneratedDocumentation saved = aiProcessingService.processExtraction(
            "user-123",
            "source-123",
            "source body"
        );

        ArgumentCaptor<GeneratedDocumentation> captor = ArgumentCaptor.forClass(GeneratedDocumentation.class);
        verify(generatedDocumentationRepository, times(2)).save(captor.capture());

        GeneratedDocumentation persisted = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(GeneratedDocumentation.ProcessingStatus.COMPLETED);
        assertThat(persisted.getUserId()).isEqualTo("user-123");
        assertThat(persisted.getSourceContentId()).isEqualTo("source-123");
        assertThat(persisted.getKeyChanges()).isEqualTo("- Added OAuth login");
        assertThat(persisted.getActionItems()).isEqualTo("- Update docs");
        assertThat(persisted.getCompletedAt()).isNotNull();
    }

    @Test
    void shouldPersistControlledFailureWhenExtractionResponseIsMalformed() {
        when(aiExtractionClient.extractDocumentation("bad source"))
            .thenThrow(new IllegalStateException("Malformed AI response"));
        when(generatedDocumentationRepository.save(any(GeneratedDocumentation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        GeneratedDocumentation saved = aiProcessingService.processExtraction(
            "user-123",
            "source-999",
            "bad source"
        );

        ArgumentCaptor<GeneratedDocumentation> captor = ArgumentCaptor.forClass(GeneratedDocumentation.class);
        verify(generatedDocumentationRepository, times(2)).save(captor.capture());

        GeneratedDocumentation persisted = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(GeneratedDocumentation.ProcessingStatus.FAILED);
        assertThat(persisted.getProcessingError()).contains("Malformed AI response");
        assertThat(persisted.getCompletedAt()).isNotNull();
        assertThat(persisted.getKeyChanges()).isNull();
        assertThat(persisted.getActionItems()).isNull();
    }
}