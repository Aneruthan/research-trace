package com.researchtrace.backend.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void malformedModelOutputIsNotAnEmptySuccess() {
        TaskExtractionInput input = notes("Alex will measure the gap.");

        assertThatThrownBy(() -> GeminiTaskExtractor.tasksFromResponse(null, input))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("malformed");
        assertThatThrownBy(() -> GeminiTaskExtractor.tasksFromResponse(
                new GeminiTaskExtractor.GeminiTaskExtractionResponse(null), input))
            .isInstanceOf(TaskExtractionException.class);
    }

    @Test
    void anEmptyTaskListIsASuccessfulResultWithNoSuggestions() {
        assertThat(GeminiTaskExtractor.tasksFromResponse(
            new GeminiTaskExtractor.GeminiTaskExtractionResponse(List.of()),
            notes("No follow-up was assigned."))).isEmpty();
    }

    @Test
    void structuredOutputDropsUnknownAssigneesMilestonesAndDates() {
        List<ExtractedTask> tasks = GeminiTaskExtractor.tasksFromResponse(
            new GeminiTaskExtractor.GeminiTaskExtractionResponse(List.of(
                new GeminiTaskExtractor.GeminiTaskDto(
                    "Measure the gap",
                    "Compare the two models",
                    "Alex",
                    7L,
                    99L,
                    "next week",
                    "Alex will measure the gap.",
                    0.8),
                new GeminiTaskExtractor.GeminiTaskDto("  ", null, null, null, null, null, null, null))),
            notes("Alex will measure the gap."));

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.title()).isEqualTo("Measure the gap");
            assertThat(task.description()).isEqualTo("Compare the two models");
            assertThat(task.assigneeName()).isEqualTo("Alex");
            assertThat(task.assigneeId()).isEqualTo(7L);
            assertThat(task.milestoneId()).isNull();
            assertThat(task.dueDate()).isNull();
            assertThat(task.sourceExcerpt()).isEqualTo("Alex will measure the gap.");
        });
    }

    private static TaskExtractionInput notes(String text) {
        return new TaskExtractionInput(
            "Weekly sync",
            LocalDate.of(2026, 10, 2),
            text,
            List.of(new TaskExtractionInput.StudentCandidate(7L, "Alex")),
            List.of());
    }
}
