package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDeadline;
import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResearchChange;
import com.researchtrace.backend.dto.research.ExtractedResearchEntity;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractedTask;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ResearchExtractionChecks {

    private ResearchExtractionChecks() {
    }

    static void requireNotes(String meetingNotes) {
        if (meetingNotes == null || meetingNotes.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Meeting notes are required for Gemini research extraction");
        }
    }

    static void validateModelResponse(MeetingExtractionResponse response) {
        if (response == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Gemini returned no research extraction");
        }
        requireConfidences(response);
    }

    private static void requireConfidences(MeetingExtractionResponse response) {
        if (response.getTasks() != null) {
            for (ExtractedTask task : response.getTasks()) {
                requireConfidence(task == null ? null : task.getConfidence());
            }
        }
        if (response.getDecisions() != null) {
            for (ExtractedDecision decision : response.getDecisions()) {
                requireConfidence(decision == null ? null : decision.getConfidence());
            }
        }
        if (response.getExperiments() != null) {
            for (ExtractedExperiment experiment : response.getExperiments()) {
                requireConfidence(experiment == null ? null : experiment.getConfidence());
            }
        }
        if (response.getResults() != null) {
            for (ExtractedResult result : response.getResults()) {
                requireConfidence(result == null ? null : result.getConfidence());
            }
        }
        if (response.getDeadlines() != null) {
            for (ExtractedDeadline deadline : response.getDeadlines()) {
                requireConfidence(deadline == null ? null : deadline.getConfidence());
            }
        }
        if (response.getResearchEntities() != null) {
            for (ExtractedResearchEntity entity : response.getResearchEntities()) {
                requireConfidence(entity == null ? null : entity.getConfidence());
            }
        }
        if (response.getResearchChanges() != null) {
            for (ExtractedResearchChange change : response.getResearchChanges()) {
                requireConfidence(change == null ? null : change.getConfidence());
            }
        }
    }

    private static void requireConfidence(Double confidence) {
        if (confidence == null) {
            return;
        }
        if (confidence.isNaN() || confidence.isInfinite() || confidence < 0.0d || confidence > 1.0d) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Extraction confidence must be between 0 and 1");
        }
    }
}
