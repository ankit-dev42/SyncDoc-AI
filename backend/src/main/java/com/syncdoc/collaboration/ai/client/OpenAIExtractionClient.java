package com.syncdoc.collaboration.ai.client;

import com.syncdoc.collaboration.ai.service.AIProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test")
public class OpenAIExtractionClient implements AIProcessingService.AIExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIExtractionClient.class);

    private static final String PROMPT_TEMPLATE =
        "You are a technical documentation assistant. Analyze the following code change and extract structured information.\n\n" +
        "Code change content:\n{sourceContent}\n\n" +
        "Provide your analysis in exactly this format:\n\n" +
        "KEY_CHANGES:\n- [Each significant code change as a bullet point]\n\n" +
        "ACTION_ITEMS:\n- [Each recommended follow-up task as a bullet point]";

    private final ChatClient chatClient;

    public OpenAIExtractionClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public String extractDocumentation(String sourceContent) {
        String prompt = PROMPT_TEMPLATE.replace("{sourceContent}", sourceContent);
        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();
        log.debug("OpenAI extraction response received, length={}", response != null ? response.length() : 0);
        return response != null ? response : "";
    }
}
