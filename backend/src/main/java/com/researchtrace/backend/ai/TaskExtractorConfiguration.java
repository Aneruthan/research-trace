package com.researchtrace.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TaskExtractorProperties.class)
public class TaskExtractorConfiguration {

    @Bean
    @ConditionalOnProperty(name = "task-extractor.mode", havingValue = "stub", matchIfMissing = true)
    public TaskExtractor stubTaskExtractor() {
        return new StubTaskExtractor();
    }

    @Bean
    @ConditionalOnProperty(name = "task-extractor.mode", havingValue = "llm")
    public TaskExtractor llmTaskExtractor(TaskExtractorProperties properties) {
        return new LlmTaskExtractor(properties);
    }
}
