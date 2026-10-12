package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnconfiguredResearchNotesExtractor implements ResearchNotesExtractor {

    @Override
    public MeetingExtractionResponse extract(String meetingNotes) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Gemini research extraction is not configured");
    }
}
