package com.researchtrace.backend.research;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            .contains("confidence value between 0 and 1");
    }
}
