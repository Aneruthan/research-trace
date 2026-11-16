package com.researchtrace.backend.research;

import com.researchtrace.backend.ai.GeminiConfigurationValidator;
import com.researchtrace.backend.ai.GeminiModeConditions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResearchExtractionConfiguration {

    @Bean
    @Conditional(GeminiModeConditions.ResearchGemini.class)
    public ResearchNotesExtractor geminiResearchNotesExtractor(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        GeminiConfigurationValidator.validateApiKey(apiKey, "research-extractor.mode");
        return new GeminiResearchNotesExtractor(chatClientBuilder.build());
    }

    @Bean
    @Conditional(GeminiModeConditions.ResearchUnconfigured.class)
    public ResearchNotesExtractor unconfiguredResearchNotesExtractor() {
        return new UnconfiguredResearchNotesExtractor();
    }

    @Bean
    @ConditionalOnProperty(name = "research-extractor.mode", havingValue = "stub")
    public ResearchNotesExtractor stubResearchNotesExtractor() {
        return new StubResearchNotesExtractor();
    }
}
