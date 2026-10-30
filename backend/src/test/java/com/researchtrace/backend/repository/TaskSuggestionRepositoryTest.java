package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskSuggestionRepositoryTest {

    private static final Long MEETING_ID = 21L;
    private static final Long OTHER_MEETING_ID = 22L;
    private static final Long PROJECT_ID = 1L;

    @Autowired
    private TaskSuggestionRepository taskSuggestionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsAllFields() {
        TaskSuggestion suggestion = suggestion(MEETING_ID, TaskSuggestionStatus.ACCEPTED, "Run baseline");
        suggestion.setDescription("Evaluate Qwen-7B on the dev split");
        suggestion.setAssigneeName("Aneruthan");
        suggestion.setAssigneeId(7L);
        suggestion.setMilestoneId(11L);
        suggestion.setDueDate(LocalDate.of(2026, 10, 9));
        suggestion.setSourceExcerpt("Aneruthan will run the Qwen baseline by next Friday");
        suggestion.setConfidence(new BigDecimal("0.85"));
        suggestion.setAcceptedTaskId(99L);

        Long id = taskSuggestionRepository.save(suggestion).getId();
        entityManager.flush();
        entityManager.clear();

        TaskSuggestion reloaded = taskSuggestionRepository.findById(id).orElseThrow();

        assertThat(reloaded.getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(reloaded.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(reloaded.getTitle()).isEqualTo("Run baseline");
        assertThat(reloaded.getDescription()).isEqualTo("Evaluate Qwen-7B on the dev split");
        assertThat(reloaded.getAssigneeName()).isEqualTo("Aneruthan");
        assertThat(reloaded.getAssigneeId()).isEqualTo(7L);
        assertThat(reloaded.getMilestoneId()).isEqualTo(11L);
        assertThat(reloaded.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(reloaded.getSourceExcerpt())
            .isEqualTo("Aneruthan will run the Qwen baseline by next Friday");
        assertThat(reloaded.getConfidence()).isEqualByComparingTo("0.85");
        assertThat(reloaded.getStatus()).isEqualTo(TaskSuggestionStatus.ACCEPTED);
        assertThat(reloaded.getAcceptedTaskId()).isEqualTo(99L);
    }

    @Test
    void keepsOptionalFieldsNullWhenNotProvided() {
        Long id = taskSuggestionRepository
            .save(suggestion(MEETING_ID, TaskSuggestionStatus.PENDING, "Write related work"))
            .getId();
        entityManager.flush();
        entityManager.clear();

        TaskSuggestion reloaded = taskSuggestionRepository.findById(id).orElseThrow();

        assertThat(reloaded.getAssigneeId()).isNull();
        assertThat(reloaded.getMilestoneId()).isNull();
        assertThat(reloaded.getDueDate()).isNull();
        assertThat(reloaded.getConfidence()).isNull();
        assertThat(reloaded.getAcceptedTaskId()).isNull();
    }

    @Test
    void findsSuggestionsOfOneMeetingOnly() {
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.PENDING, "Run baseline"));
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.REJECTED, "Order GPUs"));
        taskSuggestionRepository.save(suggestion(OTHER_MEETING_ID, TaskSuggestionStatus.PENDING, "Other meeting"));
        entityManager.flush();
        entityManager.clear();

        List<TaskSuggestion> suggestions = taskSuggestionRepository.findByMeetingId(MEETING_ID);

        assertThat(suggestions)
            .extracting(TaskSuggestion::getTitle)
            .containsExactlyInAnyOrder("Run baseline", "Order GPUs");
    }

    @Test
    void deletesOnlyPendingSuggestionsForTheMeeting() {
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.PENDING, "Pending one"));
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.PENDING, "Pending two"));
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.ACCEPTED, "Accepted"));
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.REJECTED, "Rejected"));
        taskSuggestionRepository.save(suggestion(OTHER_MEETING_ID, TaskSuggestionStatus.PENDING, "Other pending"));
        entityManager.flush();
        entityManager.clear();

        taskSuggestionRepository.deleteByMeetingIdAndStatus(MEETING_ID, TaskSuggestionStatus.PENDING);
        entityManager.flush();
        entityManager.clear();

        assertThat(taskSuggestionRepository.findByMeetingId(MEETING_ID))
            .extracting(TaskSuggestion::getStatus)
            .containsExactlyInAnyOrder(TaskSuggestionStatus.ACCEPTED, TaskSuggestionStatus.REJECTED);
        assertThat(taskSuggestionRepository.findByMeetingId(OTHER_MEETING_ID))
            .extracting(TaskSuggestion::getTitle)
            .containsExactly("Other pending");
    }

    @Test
    void preservesAcceptedAndRejectedSuggestions() {
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.ACCEPTED, "Accepted"));
        taskSuggestionRepository.save(suggestion(MEETING_ID, TaskSuggestionStatus.REJECTED, "Rejected"));
        entityManager.flush();
        entityManager.clear();

        taskSuggestionRepository.deleteByMeetingIdAndStatus(MEETING_ID, TaskSuggestionStatus.PENDING);
        entityManager.flush();
        entityManager.clear();

        assertThat(taskSuggestionRepository.findByMeetingId(MEETING_ID))
            .extracting(TaskSuggestion::getTitle)
            .containsExactlyInAnyOrder("Accepted", "Rejected");
    }

    private TaskSuggestion suggestion(Long meetingId, TaskSuggestionStatus status, String title) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setMeetingId(meetingId);
        suggestion.setProjectId(PROJECT_ID);
        suggestion.setTitle(title);
        suggestion.setStatus(status);
        return suggestion;
    }
}
