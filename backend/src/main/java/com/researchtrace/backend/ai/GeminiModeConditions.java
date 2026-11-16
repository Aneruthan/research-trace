package com.researchtrace.backend.ai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Selects a Gemini extractor only when that mode is active and an API key is present.
 * The same mode with a blank key selects the unconfigured extractor, which reports 503.
 */
public final class GeminiModeConditions {

    private GeminiModeConditions() {
    }

    public static class TaskGemini extends ModeCondition {
        public TaskGemini() {
            super("task-extractor.mode", true);
        }
    }

    public static class TaskUnconfigured extends ModeCondition {
        public TaskUnconfigured() {
            super("task-extractor.mode", false);
        }
    }

    public static class ResearchGemini extends ModeCondition {
        public ResearchGemini() {
            super("research-extractor.mode", true);
        }
    }

    public static class ResearchUnconfigured extends ModeCondition {
        public ResearchUnconfigured() {
            super("research-extractor.mode", false);
        }
    }

    private static class ModeCondition implements Condition {
        private final String modeProperty;
        private final boolean keyRequired;

        private ModeCondition(String modeProperty, boolean keyRequired) {
            this.modeProperty = modeProperty;
            this.keyRequired = keyRequired;
        }

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String mode = context.getEnvironment().getProperty(modeProperty, "gemini");
            if ("research-extractor.mode".equals(modeProperty) && "unconfigured".equals(mode)) {
                return !keyRequired;
            }
            if (!"gemini".equals(mode)) {
                return false;
            }
            String key = context.getEnvironment().getProperty("spring.ai.google.genai.api-key", "");
            boolean present = key != null && !key.isBlank();
            return present == keyRequired;
        }
    }
}
