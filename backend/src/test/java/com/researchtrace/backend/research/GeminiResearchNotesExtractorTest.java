package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiResearchNotesExtractorTest {

    @Test
    void promptAsksForResearchCategoriesWithoutInventingFacts() {
        assertThat(GeminiResearchNotesExtractor.SYSTEM_PROMPT)
            .contains("Tasks")
            .contains("Decisions")
            .contains("Experiments")
            .contains("Results")
            .contains("Deadlines")
            .contains("Research entities")
            .contains("Possible research direction changes")
            .contains("Do not invent information")
            .contains("confidence value between 0 and 1")
            .contains("set method only when the notes explicitly describe")
            .contains("set comparison only when the notes explicitly compare")
            .contains("set madeBy only when the notes explicitly name");
    }

    @Test
    void blankNotesAreRejectedBeforeTheModelIsCalled() {
        Probe extractor = new Probe();

        assertThatThrownBy(() -> extractor.extract("   "))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException exception = (ResponseStatusException) error;
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(exception.getReason()).contains("Meeting notes are required");
            });
        assertThat(extractor.called).isFalse();
    }

    @Test
    void malformedModelOutputBecomesAGatewayErrorAndDoesNotUseStubContent() {
        Probe extractor = new Probe();
        extractor.failure = new IllegalStateException("could not map the model JSON");

        assertThatThrownBy(() -> extractor.extract("Discuss the baseline."))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException exception = (ResponseStatusException) error;
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(exception.getReason()).isEqualTo("Gemini research extraction failed");
                assertThat(exception.getReason()).doesNotContain("stub");
            });
    }

    @Test
    void aConfidenceOutsideZeroToOneIsRejected() {
        Probe extractor = new Probe();
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        decision.setConfidence(1.5d);
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        response.setDecisions(List.of(decision));
        extractor.response = response;

        assertThatThrownBy(() -> extractor.extract("Discuss the baseline."))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException exception = (ResponseStatusException) error;
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(exception.getReason()).contains("between 0 and 1");
            });
    }

    @Test
    void anEmptyModelResponseIsRejected() {
        Probe extractor = new Probe();

        assertThatThrownBy(() -> extractor.extract("Discuss the baseline."))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void aValidModelResponseIsReturnedUnchanged() {
        Probe extractor = new Probe();
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        response.setDecisions(List.of());
        extractor.response = response;

        assertThat(extractor.extract("Discuss the baseline.")).isSameAs(response);
    }

    private static final class Probe extends GeminiResearchNotesExtractor {
        private boolean called;
        private RuntimeException failure;
        private MeetingExtractionResponse response;

        private Probe() {
            super(null);
        }

        @Override
        MeetingExtractionResponse callModel(String meetingNotes) {
            called = true;
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
