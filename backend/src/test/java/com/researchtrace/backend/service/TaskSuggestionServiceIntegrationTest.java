package com.researchtrace.backend.service;

import com.researchtrace.backend.ai.StubTaskExtractor;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import({TaskSuggestionService.class, TaskService.class, StubTaskExtractor.class, ProgressService.class, MilestoneService.class})
class TaskSuggestionServiceIntegrationTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;

    @Autowired
    private TaskSuggestionService taskSuggestionService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSuggestionRepository taskSuggestionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void reExtractionReplacesPendingSuggestionsAndKeepsReviewedOnes() {
        Long aneruthanId = student(PROJECT_ID, "Aneruthan");
        Long gayathriId = student(PROJECT_ID, "Gayathri");
        milestone(PROJECT_ID, "Baseline evaluation", "100");
        Long meetingId = meeting(PROJECT_ID, """
            Weekly sync notes
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05 | Baseline evaluation
            TODO: Gayathri | Prepare dataset statistics | 2026-10-06
            ACTION: Write related work
            """);

        List<TaskSuggestion> first = taskSuggestionService.extract(meetingId);
        assertThat(first).hasSize(3).allMatch(s -> s.getStatus() == TaskSuggestionStatus.PENDING);
        assertThat(first).extracting(TaskSuggestion::getAssigneeId)
            .containsExactly(aneruthanId, gayathriId, null);

        taskSuggestionService.accept(first.get(0).getId(), null);
        taskSuggestionService.reject(first.get(1).getId());
        flushAndClear();

        taskSuggestionService.extract(meetingId);
        flushAndClear();

        List<TaskSuggestion> afterReExtraction = taskSuggestionService.listByMeeting(meetingId);
        assertThat(afterReExtraction)
            .extracting(TaskSuggestion::getTitle, TaskSuggestion::getStatus)
            .containsExactlyInAnyOrder(
                tuple("Run Qwen baseline", TaskSuggestionStatus.ACCEPTED),
                tuple("Prepare dataset statistics", TaskSuggestionStatus.REJECTED),
                tuple("Prepare dataset statistics", TaskSuggestionStatus.PENDING),
                tuple("Write related work", TaskSuggestionStatus.PENDING));
        assertThat(taskSuggestionRepository.findById(first.get(2).getId())).isEmpty();
        assertThat(taskRepository.count()).isEqualTo(1);
    }

    @Test
    void acceptingTheSameSuggestionTwiceDoesNotCreateASecondTask() {
        student(PROJECT_ID, "Aneruthan");
        Long meetingId = meeting(PROJECT_ID, "ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");

        TaskSuggestion suggestion = taskSuggestionService.extract(meetingId).get(0);
        taskSuggestionService.accept(suggestion.getId(), null);
        assertThat(taskRepository.count()).isEqualTo(1);

        assertThatThrownBy(() -> taskSuggestionService.accept(suggestion.getId(), null))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void notesWithoutAnActionLineAreASuccessfulEmptyExtraction() {
        student(PROJECT_ID, "Aneruthan");
        Long meetingId = meeting(PROJECT_ID, "We discussed the literature and did not assign follow-up work.");

        List<TaskSuggestion> suggestions = taskSuggestionService.extract(meetingId);

        assertThat(suggestions).isEmpty();
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void acceptedSuggestionsBecomeTasksThatDriveMilestoneAndProjectProgress() {
        student(PROJECT_ID, "Aneruthan");
        student(PROJECT_ID, "Gayathri");
        Long milestoneId = milestone(PROJECT_ID, "Baseline evaluation", "100");
        Long meetingId = meeting(PROJECT_ID, """
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05 | Baseline evaluation
            ACTION: Gayathri | Prepare dataset statistics | 2026-10-06 | Baseline evaluation
            """);

        List<TaskSuggestion> suggestions = taskSuggestionService.extract(meetingId);
        assertThat(taskRepository.count()).isZero();

        Task first = taskSuggestionService.accept(suggestions.get(0).getId(), null);
        taskSuggestionService.accept(suggestions.get(1).getId(), null);
        flushAndClear();

        assertThat(first.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(first.getMeetingId()).isEqualTo(meetingId);
        assertThat(first.getTaskSuggestionId()).isEqualTo(suggestions.get(0).getId());
        assertThat(first.getCreatedAt()).isNotNull();
        assertThat(first.getUpdatedAt()).isNotNull();

        assertThat(taskRepository.findByProjectId(PROJECT_ID)).hasSize(2);
        assertThat(taskRepository.findByMilestoneId(milestoneId))
            .hasSize(2)
            .allMatch(task -> task.getStatus() == TaskStatus.TODO);
        assertThat(progressService.getMilestoneProgress(milestoneId)).isEqualByComparingTo("0.00");

        Task completed = taskRepository.findById(first.getId()).orElseThrow();
        completed.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(completed);
        flushAndClear();

        assertThat(progressService.getMilestoneProgress(milestoneId)).isEqualByComparingTo("50.00");
        assertThat(progressService.getProjectProgress(PROJECT_ID)).isEqualByComparingTo("50.00");
    }

    @Test
    void extractionNeverLinksStudentsOrMilestonesFromAnotherProject() {
        student(PROJECT_ID, "Aneruthan");
        student(OTHER_PROJECT_ID, "Outsider");
        milestone(OTHER_PROJECT_ID, "Other project milestone", "100");
        Long meetingId = meeting(PROJECT_ID,
            "ACTION: Outsider | Borrow other project's work | 2026-10-05 | Other project milestone");

        List<TaskSuggestion> suggestions = taskSuggestionService.extract(meetingId);

        assertThat(suggestions).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.getAssigneeName()).isEqualTo("Outsider");
            assertThat(suggestion.getAssigneeId()).isNull();
            assertThat(suggestion.getMilestoneId()).isNull();
        });
    }

    private Long student(Long projectId, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setRole(UserRole.STUDENT);
        Long userId = userRepository.save(user).getId();

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(ProjectMemberRole.STUDENT);
        projectMemberRepository.save(member);
        return userId;
    }

    private Long milestone(Long projectId, String title, String weight) {
        Milestone milestone = new Milestone();
        milestone.setProjectId(projectId);
        milestone.setTitle(title);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setWeight(new BigDecimal(weight));
        return milestoneRepository.save(milestone).getId();
    }

    private Long meeting(Long projectId, String notes) {
        Meeting meeting = new Meeting();
        meeting.setProjectId(projectId);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 2));
        meeting.setNotes(notes);
        Long id = meetingRepository.save(meeting).getId();
        flushAndClear();
        return id;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
