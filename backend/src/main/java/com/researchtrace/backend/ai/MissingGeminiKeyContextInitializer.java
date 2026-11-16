package com.researchtrace.backend.ai;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Google GenAI chat auto-configuration refuses to start when its mode is selected and the API key
 * is blank. This runs after command-line and test properties are in place, and switches that mode
 * off so the application can start. Extraction then uses the unconfigured extractor and returns 503.
 * Stub mode is unchanged.
 */
public class MissingGeminiKeyContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String key = environment.getProperty("spring.ai.google.genai.api-key", "");
        String chat = environment.getProperty("spring.ai.model.chat", "google-genai");
        if (key != null && !key.isBlank()) {
            return;
        }
        if (!"google-genai".equals(chat)) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(
            "missingGeminiKey",
            Map.of("spring.ai.model.chat", "none")));
    }
}
