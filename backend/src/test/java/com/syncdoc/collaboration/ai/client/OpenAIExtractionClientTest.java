package com.syncdoc.collaboration.ai.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("OpenAIExtractionClient unit tests")
class OpenAIExtractionClientTest {

    @Test
    @DisplayName("extractDocumentation returns response from ChatClient")
    void extractDocumentation_returnsResponseFromChatClient() {
        ChatClient chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(
            "KEY_CHANGES:\n- Added retry logic\nACTION_ITEMS:\n- Update docs"
        );

        OpenAIExtractionClient client = new OpenAIExtractionClient(chatClient);
        String result = client.extractDocumentation("diff content here");

        assertThat(result).contains("KEY_CHANGES");
    }

    @Test
    @DisplayName("extractDocumentation handles null AI response gracefully")
    void extractDocumentation_nullAiResponse_returnsEmpty() {
        ChatClient chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(null);

        OpenAIExtractionClient client = new OpenAIExtractionClient(chatClient);
        String result = client.extractDocumentation("minimal content");

        assertThat(result).isEmpty();
    }
}
