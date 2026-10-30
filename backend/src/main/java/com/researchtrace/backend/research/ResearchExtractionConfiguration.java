package com.researchtrace.backend.research;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResearchExtractionConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
    public ResearchNotesExtractor geminiResearchNotesExtractor(ChatClient.Builder chatClientBuilder) {
        return new GeminiResearchNotesExtractor(chatClientBuilder.build());
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
    public ResearchNotesExtractor unconfiguredResearchNotesExtractor() {
        return new UnconfiguredResearchNotesExtractor();
    }
}
