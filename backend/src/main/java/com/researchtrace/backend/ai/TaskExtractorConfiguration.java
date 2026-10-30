package com.researchtrace.backend.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TaskExtractorProperties.class)
public class TaskExtractorConfiguration {

    @Bean
    @ConditionalOnProperty(name = "task-extractor.mode", havingValue = "stub")
    public TaskExtractor stubTaskExtractor() {
        return new StubTaskExtractor();
    }

    @Bean
    @ConditionalOnProperty(name = "task-extractor.mode", havingValue = "gemini", matchIfMissing = true)
    public TaskExtractor geminiTaskExtractor(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        GeminiConfigurationValidator.validateApiKey(apiKey, "task-extractor.mode");
        return new GeminiTaskExtractor(chatClientBuilder.build());
    }

    @Bean
    @ConditionalOnProperty(name = "task-extractor.mode", havingValue = "llm")
    public TaskExtractor llmTaskExtractor(TaskExtractorProperties properties) {
        return new LlmTaskExtractor(properties);
    }
}
