package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubResearchNotesExtractorTest {

    private StubResearchNotesExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StubResearchNotesExtractor();
    }

    @Test
    void extractsDeterministicEntitiesDecisionsExperimentsAndResults() {
        String notes = """
            Meeting on Medical VQA Evaluation:
            We decided to adopt the PMC-VQA benchmark for visual question answering.
            Aneruthan tested the zero-shot baseline with Med-CLIP and achieved 58.4% accuracy, below expectation.
            We agreed to switch to LitMedImage dataset due to label noise in PMC-VQA.
            """;

        MeetingExtractionResponse response = extractor.extract(notes);

        assertThat(response).isNotNull();
        assertThat(response.getDecisions()).isNotEmpty();
        assertThat(response.getExperiments()).isNotEmpty();
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResearchEntities()).isNotEmpty();
        assertThat(response.getResearchChanges()).isNotEmpty();

        assertThat(response.getResearchEntities())
            .extracting("name")
            .contains("PMC-VQA", "LitMedImage", "Med-CLIP");

        assertThat(response.getDecisions().get(0).getTitle()).contains("LitMedImage");
        assertThat(response.getExperiments().get(0).getTitle()).contains("Baseline");
        assertThat(response.getResults().get(0).getMetrics()).contains("58.4%");
        assertThat(response.getResearchChanges().get(0).getPreviousApproach()).contains("PMC-VQA");
    }

    @Test
    void returnsEmptyCollectionsForBlankNotes() {
        MeetingExtractionResponse emptyResponse = extractor.extract("   ");
        assertThat(emptyResponse.getDecisions()).isEmpty();
        assertThat(emptyResponse.getExperiments()).isEmpty();
        assertThat(emptyResponse.getResults()).isEmpty();
        assertThat(emptyResponse.getResearchEntities()).isEmpty();
        assertThat(emptyResponse.getResearchChanges()).isEmpty();
    }
}
