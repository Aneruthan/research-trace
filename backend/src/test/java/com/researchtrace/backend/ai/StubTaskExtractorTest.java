package com.researchtrace.backend.ai;

import com.researchtrace.backend.ai.TaskExtractionInput.MilestoneCandidate;
import com.researchtrace.backend.ai.TaskExtractionInput.StudentCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StubTaskExtractorTest {

    private static final List<StudentCandidate> STUDENTS = List.of(
        new StudentCandidate(7L, "Aneruthan"),
        new StudentCandidate(8L, "Gayathri"));

    private static final List<MilestoneCandidate> MILESTONES = List.of(
        new MilestoneCandidate(11L, "Baseline evaluation"),
        new MilestoneCandidate(12L, "Dataset preparation"));

    private final StubTaskExtractor extractor = new StubTaskExtractor();

    @Test
    void extractsOneValidActionLine() {
        List<ExtractedTask> tasks = extract(
            "ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");

        assertThat(tasks).containsExactly(new ExtractedTask(
            "Run Qwen baseline",
            null,
            "Aneruthan",
            7L,
            null,
            LocalDate.of(2026, 10, 5),
            "ACTION: Aneruthan | Run Qwen baseline | 2026-10-05",
            new BigDecimal("1.00")));
    }

    @Test
    void extractsMultipleTaskLinesInOrder() {
        List<ExtractedTask> tasks = extract("""
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05
            ACTION: Gayathri | Clean the evaluation split | 2026-10-07
            ACTION: Aneruthan | Write results section | 2026-10-08
            """);

        assertThat(tasks)
            .extracting(ExtractedTask::title)
            .containsExactly(
                "Run Qwen baseline",
                "Clean the evaluation split",
                "Write results section");
    }

    @Test
    void recognizesBothTodoAndActionPrefixes() {
        List<ExtractedTask> tasks = extract("""
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05
            TODO: Gayathri | Prepare dataset statistics | 2026-10-06
            """);

        assertThat(tasks)
            .extracting(ExtractedTask::title, ExtractedTask::assigneeId, ExtractedTask::dueDate)
            .containsExactly(
                tuple("Run Qwen baseline", 7L, LocalDate.of(2026, 10, 5)),
                tuple("Prepare dataset statistics", 8L, LocalDate.of(2026, 10, 6)));
    }

    @Test
    void ignoresMalformedAndIrrelevantLines() {
        List<ExtractedTask> tasks = extract("""
            Weekly sync with Prof. Verma
            We discussed the baseline results.
            Aneruthan will look into the dataset.
            ACTION:
            ACTION: Aneruthan |  | 2026-10-05
            TODO: a | b | 2026-10-05 | Baseline evaluation | extra
            DECISION: Use Qwen-7B | Better accuracy
            NOTE ACTION: Aneruthan | Not at line start | 2026-10-05
            ACTION: Gayathri | Prepare dataset statistics | 2026-10-06
            """);

        assertThat(tasks)
            .extracting(ExtractedTask::title)
            .containsExactly("Prepare dataset statistics");
    }

    @Test
    void handlesMissingOptionalValuesSafely() {
        List<ExtractedTask> tasks = extract("""
            ACTION: Run literature search
            ACTION:  | Summarise related work |
            TODO: Gayathri | Prepare dataset statistics
            TODO: Unknown Person | Order GPUs | not-a-date
            """);

        assertThat(tasks).hasSize(4);

        ExtractedTask titleOnly = tasks.get(0);
        assertThat(titleOnly.title()).isEqualTo("Run literature search");
        assertThat(titleOnly.assigneeName()).isNull();
        assertThat(titleOnly.assigneeId()).isNull();
        assertThat(titleOnly.dueDate()).isNull();
        assertThat(titleOnly.milestoneId()).isNull();

        ExtractedTask blankAssignee = tasks.get(1);
        assertThat(blankAssignee.title()).isEqualTo("Summarise related work");
        assertThat(blankAssignee.assigneeName()).isNull();
        assertThat(blankAssignee.dueDate()).isNull();

        ExtractedTask noDate = tasks.get(2);
        assertThat(noDate.assigneeId()).isEqualTo(8L);
        assertThat(noDate.dueDate()).isNull();

        ExtractedTask unknownAssigneeAndBadDate = tasks.get(3);
        assertThat(unknownAssigneeAndBadDate.assigneeName()).isEqualTo("Unknown Person");
        assertThat(unknownAssigneeAndBadDate.assigneeId()).isNull();
        assertThat(unknownAssigneeAndBadDate.dueDate()).isNull();
    }

    @Test
    void returnsEmptyListForNullOrBlankNotes() {
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract(input(null))).isEmpty();
        assertThat(extractor.extract(input("   "))).isEmpty();
    }

    @Test
    void matchesMilestoneTitleAndAssigneeCaseInsensitively() {
        List<ExtractedTask> tasks = extract(
            "action: aneruthan | Run Qwen baseline | 2026-10-05 | baseline EVALUATION");

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.assigneeId()).isEqualTo(7L);
            assertThat(task.milestoneId()).isEqualTo(11L);
        });
    }

    @Test
    void leavesIdsNullWhenNamesAreUnknownOrAmbiguous() {
        TaskExtractionInput input = new TaskExtractionInput(
            "Weekly sync",
            LocalDate.of(2026, 10, 2),
            "ACTION: Sam | Run Qwen baseline | 2026-10-05 | Unknown milestone",
            List.of(new StudentCandidate(7L, "Sam"), new StudentCandidate(9L, "Sam")),
            MILESTONES);

        assertThat(extractor.extract(input)).singleElement().satisfies(task -> {
            assertThat(task.assigneeName()).isEqualTo("Sam");
            assertThat(task.assigneeId()).isNull();
            assertThat(task.milestoneId()).isNull();
        });
    }

    @Test
    void isDeterministic() {
        TaskExtractionInput input = input("""
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05 | Baseline evaluation
            TODO: Gayathri | Prepare dataset statistics | 2026-10-06 | Dataset preparation
            """);

        List<ExtractedTask> first = extractor.extract(input);
        List<ExtractedTask> second = extractor.extract(input);
        List<ExtractedTask> fromNewInstance = new StubTaskExtractor().extract(input);

        assertThat(first).hasSize(2);
        assertThat(second).isEqualTo(first);
        assertThat(fromNewInstance).isEqualTo(first);
    }

    @Test
    void hasNoRepositoryOrDatabaseDependencies() {
        for (Constructor<?> constructor : StubTaskExtractor.class.getDeclaredConstructors()) {
            assertThat(constructor.getParameterCount()).isZero();
        }

        List<Field> instanceFields = Arrays.stream(StubTaskExtractor.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .toList();
        assertThat(instanceFields).isEmpty();

        for (Field field : StubTaskExtractor.class.getDeclaredFields()) {
            assertThat(Repository.class.isAssignableFrom(field.getType())).isFalse();
            assertThat(field.getType().getPackageName())
                .doesNotStartWith("com.researchtrace.backend.repository")
                .doesNotStartWith("com.researchtrace.backend.model");
        }
    }

    private List<ExtractedTask> extract(String notes) {
        return extractor.extract(input(notes));
    }

    private TaskExtractionInput input(String notes) {
        return new TaskExtractionInput(
            "Weekly sync",
            LocalDate.of(2026, 10, 2),
            notes,
            STUDENTS,
            MILESTONES);
    }
}
