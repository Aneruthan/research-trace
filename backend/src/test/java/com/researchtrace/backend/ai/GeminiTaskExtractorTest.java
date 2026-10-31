package com.researchtrace.backend.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiTaskExtractorTest {

    @Test
    void promptDemandsStructuredTasksAndPreservesCandidates() {
        assertThat(GeminiTaskExtractor.SYSTEM_PROMPT)
                .contains("Extract actionable research-project tasks")
                .contains("title")
                .contains("assigneeId")
                .contains("milestoneId")
                .contains("dueDate")
                .contains("sourceExcerpt")
                .contains("confidence")
                .contains("Never invent student IDs or milestone IDs");
    }

    @Test
    void emptyOrNullInputReturnsEmptyList() {
        GeminiTaskExtractor extractor = new GeminiTaskExtractor(null);
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract(new TaskExtractionInput(null, null, null, List.of(), List.of()))).isEmpty();
        assertThat(extractor.extract(new TaskExtractionInput("Title", LocalDate.now(), "   ", List.of(), List.of()))).isEmpty();
    }
}
