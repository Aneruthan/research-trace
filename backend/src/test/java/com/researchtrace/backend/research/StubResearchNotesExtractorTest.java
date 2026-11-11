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
    void extractsExplicitPrefixLinesForMultipleDecisionsExperimentsAndResults() {
        String notes = """
            Research Supervision Meeting Notes
            DECISION: Adopt ViT-B16 architecture | Captures long-range context across histology microenvironments | Standardize vision backbone
            DECISION: Enforce patient-stratified 5-fold CV | Prevents slide tile leakage across folds | Rigorous evaluation protocol
            EXPERIMENT: ViT-B16 vs ResNet-50 benchmark | Benchmark classification AUC on TCGA-BRCA | Backbone architecture | AUC > 0.95
            EXPERIMENT: Patch resolution sensitivity | Measure effect of 256x256 vs 512x512 tiles | Tile size | Higher resolution improves mitosis classification
            RESULT: ViT-B16 vs ResNet-50 benchmark | Accuracy: 93.4%, AUC: 0.96 | ViT-B16 reached 93.4% accuracy on TCGA-BRCA | +4.2% AUC over ResNet-50
            RESULT: Patch resolution sensitivity | Accuracy: 94.1%, F1: 0.92 | 512x512 tile size achieved 94.1% accuracy | 18% error reduction
            CHANGE: ResNet-50 baseline | ViT-B16 architecture | ViT provides attention maps and higher AUC | Replace convolutional backbone
            ENTITY: TCGA-BRCA | DATASET | Breast Invasive Carcinoma whole-slide pathology dataset
            ENTITY: ViT-B16 | MODEL | Vision Transformer with 16x16 patch resolution
            """;

        MeetingExtractionResponse response = extractor.extract(notes);

        assertThat(response).isNotNull();
        assertThat(response.getDecisions()).hasSize(2);
        assertThat(response.getDecisions().get(0).getTitle()).isEqualTo("Adopt ViT-B16 architecture");
        assertThat(response.getDecisions().get(1).getTitle()).isEqualTo("Enforce patient-stratified 5-fold CV");

        assertThat(response.getExperiments()).hasSize(2);
        assertThat(response.getExperiments().get(0).getTitle()).isEqualTo("ViT-B16 vs ResNet-50 benchmark");
        assertThat(response.getExperiments().get(1).getTitle()).isEqualTo("Patch resolution sensitivity");

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getExperiment()).isEqualTo("ViT-B16 vs ResNet-50 benchmark");
        assertThat(response.getResults().get(0).getMetrics()).contains("93.4%");
        assertThat(response.getResults().get(1).getExperiment()).isEqualTo("Patch resolution sensitivity");
        assertThat(response.getResults().get(1).getMetrics()).contains("94.1%");

        assertThat(response.getResearchChanges()).hasSize(1);
        assertThat(response.getResearchChanges().get(0).getPreviousApproach()).isEqualTo("ResNet-50 baseline");
        assertThat(response.getResearchChanges().get(0).getNewApproach()).isEqualTo("ViT-B16 architecture");

        assertThat(response.getResearchEntities()).hasSize(2);
        assertThat(response.getResearchEntities())
            .extracting("name")
            .contains("TCGA-BRCA", "ViT-B16");
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
