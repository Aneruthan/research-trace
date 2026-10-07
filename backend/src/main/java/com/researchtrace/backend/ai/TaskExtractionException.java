package com.researchtrace.backend.ai;

/**
 * Thrown when LLM-backed task extraction cannot complete. The extractor never
 * writes to the database; callers decide how to surface the failure.
 */
public class TaskExtractionException extends RuntimeException {

    public TaskExtractionException(String message) {
        super(message);
    }

    public TaskExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
