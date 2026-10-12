package com.researchtrace.backend.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "task-extractor")
public class TaskExtractorProperties {

    /**
     * Which TaskExtractor bean to register: {@code stub} (default) or {@code llm}.
     */
    private String mode = "stub";

    private final OpenAi openai = new OpenAi();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class OpenAi {

        /**
         * OpenAI-compatible API key. Bound from OPENAI_API_KEY. Never log this value.
         */
        private String apiKey = "";

        /**
         * Model name. Bound from OPENAI_MODEL.
         */
        private String model = "";

        /**
         * OpenAI-compatible base URL, without a trailing slash.
         */
        private String baseUrl = "https://api.openai.com";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
