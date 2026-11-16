package com.researchtrace.backend.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public class UnconfiguredTaskExtractor implements TaskExtractor {

    @Override
    public List<ExtractedTask> extract(TaskExtractionInput input) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Gemini task extraction is not configured");
    }
}
