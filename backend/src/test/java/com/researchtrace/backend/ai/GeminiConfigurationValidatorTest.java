package com.researchtrace.backend.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiConfigurationValidatorTest {

    @Test
    void acceptsValidApiKey() {
        assertThatCode(() -> GeminiConfigurationValidator.validateApiKey("ai-key-12345", "task-extractor.mode"))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullApiKey() {
        assertThatThrownBy(() -> GeminiConfigurationValidator.validateApiKey(null, "task-extractor.mode"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GEMINI_API_KEY is required when task-extractor.mode is set to 'gemini'");
    }

    @Test
    void rejectsBlankApiKey() {
        assertThatThrownBy(() -> GeminiConfigurationValidator.validateApiKey("   ", "research-extractor.mode"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GEMINI_API_KEY is required when research-extractor.mode is set to 'gemini'");
    }
}
