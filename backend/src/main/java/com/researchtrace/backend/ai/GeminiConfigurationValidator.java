package com.researchtrace.backend.ai;

/**
 * Shared validation logic for Google Gemini API configuration.
 */
public final class GeminiConfigurationValidator {

    private GeminiConfigurationValidator() {}

    /**
     * Validates that the Gemini API key is present and non-blank when a Gemini extractor mode is active.
     *
     * @param apiKey the configured API key (may be empty or null)
     * @param propertyName the configuration property that activated Gemini (e.g. "task-extractor.mode")
     * @throws IllegalStateException if the API key is missing or blank
     */
    public static void validateApiKey(String apiKey, String propertyName) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "GEMINI_API_KEY is required when " + propertyName + " is set to 'gemini'. " +
                "Please configure GEMINI_API_KEY in backend/.env or your environment."
            );
        }
    }
}
