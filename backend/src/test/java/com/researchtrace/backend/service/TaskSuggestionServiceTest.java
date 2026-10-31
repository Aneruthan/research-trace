package com.researchtrace.backend.service;

import com.researchtrace.backend.ai.ExtractedTask;
import com.researchtrace.backend.ai.TaskExtractionInput;
import com.researchtrace.backend.ai.TaskExtractionInput.MilestoneCandidate;
import com.researchtrace.backend.ai.TaskExtractionInput.StudentCandidate;
import com.researchtrace.backend.ai.TaskExtractor;
import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
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
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSuggestionServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;
    private static final Long MEETING_ID = 21L;
    private static final Long SUGGESTION_ID = 40L;
    private static final Long CREATED_TASK_ID = 500L;

    private static final Long ANERUTHAN_ID = 7L;
    private static final Long GAYATHRI_ID = 8L;
    private static final Long PROFESSOR_ID = 3L;
    private static final Long OTHER_PROJECT_STUDENT_ID = 9L;

    private static final Long BASELINE_MILESTONE_ID = 11L;
    private static final Long DATASET_MILESTONE_ID = 12L;
    private static final Long OTHER_PROJECT_MILESTONE_ID = 31L;

    private static final Map<Long, User> USERS = Map.of(
        ANERUTHAN_ID, user(ANERUTHAN_ID, "Aneruthan", UserRole.STUDENT),
        GAYATHRI_ID, user(GAYATHRI_ID, "Gayathri", UserRole.STUDENT),
        PROFESSOR_ID, user(PROFESSOR_ID, "Prof. Verma", UserRole.PROFESSOR),
        OTHER_PROJECT_STUDENT_ID, user(OTHER_PROJECT_STUDENT_ID, "Outsider", UserRole.STUDENT));

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private TaskSuggestionRepository taskSuggestionRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskExtractor taskExtractor;

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    private TaskSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new TaskSuggestionService(
            meetingRepository,
            taskSuggestionRepository,
            projectMemberRepository,
            userRepository,
            milestoneRepository,
            taskRepository,
            taskExtractor,
            researchDecisionRepository,
            experimentRepository);

        lenient().when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting(PROJECT_ID)));

        lenient().when(projectMemberRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(
            member(PROJECT_ID, ANERUTHAN_ID, ProjectMemberRole.STUDENT),
            member(PROJECT_ID, GAYATHRI_ID, ProjectMemberRole.STUDENT),
            member(PROJECT_ID, PROFESSOR_ID, ProjectMemberRole.PROFESSOR)));
        lenient().when(projectMemberRepository.findByProjectId(OTHER_PROJECT_ID)).thenReturn(List.of(
            member(OTHER_PROJECT_ID, OTHER_PROJECT_STUDENT_ID, ProjectMemberRole.STUDENT)));
        lenient().when(userRepository.findAllById(anyIterable())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            return StreamSupport.stream(ids.spliterator(), false)
                .map(USERS::get)
                .filter(Objects::nonNull)
                .toList();
        });

        Milestone baseline = milestone(BASELINE_MILESTONE_ID, PROJECT_ID, "Baseline evaluation");
        Milestone dataset = milestone(DATASET_MILESTONE_ID, PROJECT_ID, "Dataset preparation");
        Milestone otherProject = milestone(OTHER_PROJECT_MILESTONE_ID, OTHER_PROJECT_ID, "Other project");
        lenient().when(milestoneRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(baseline, dataset));
        lenient().when(milestoneRepository.findById(BASELINE_MILESTONE_ID)).thenReturn(Optional.of(baseline));
        lenient().when(milestoneRepository.findById(DATASET_MILESTONE_ID)).thenReturn(Optional.of(dataset));
        lenient().when(milestoneRepository.findById(OTHER_PROJECT_MILESTONE_ID))
            .thenReturn(Optional.of(otherProject));

        lenient().when(taskSuggestionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskSuggestionRepository.save(any(TaskSuggestion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(CREATED_TASK_ID);
            return task;
        });
    }

    // ----- Extraction -----

    @Test
    void extractionCreatesPendingSuggestionsWithValidatedFields() {
        givenExtractedTasks(new ExtractedTask(
            "  Run Qwen baseline  ",
            "  Evaluate on the dev split ",
            " Aneruthan ",
            ANERUTHAN_ID,
            BASELINE_MILESTONE_ID,
            LocalDate.of(2026, 10, 5),
            "ACTION: Aneruthan | Run Qwen baseline | 2026-10-05",
            new BigDecimal("0.857")));

        List<TaskSuggestion> suggestions = service.extract(MEETING_ID);

        assertThat(suggestions).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.getStatus()).isEqualTo(TaskSuggestionStatus.PENDING);
            assertThat(suggestion.getMeetingId()).isEqualTo(MEETING_ID);
            assertThat(suggestion.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(suggestion.getTitle()).isEqualTo("Run Qwen baseline");
            assertThat(suggestion.getDescription()).isEqualTo("Evaluate on the dev split");
            assertThat(suggestion.getAssigneeName()).isEqualTo("Aneruthan");
            assertThat(suggestion.getAssigneeId()).isEqualTo(ANERUTHAN_ID);
            assertThat(suggestion.getMilestoneId()).isEqualTo(BASELINE_MILESTONE_ID);
            assertThat(suggestion.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(suggestion.getSourceExcerpt())
                .isEqualTo("ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");
            assertThat(suggestion.getConfidence()).isEqualByComparingTo("0.86");
            assertThat(suggestion.getAcceptedTaskId()).isNull();
        });
    }

    @Test
    void extractionDoesNotCreateTasks() {
        givenExtractedTasks(extracted("Run Qwen baseline", ANERUTHAN_ID, BASELINE_MILESTONE_ID));

        service.extract(MEETING_ID);

        verifyNoInteractions(taskRepository);
    }

    @Test
    void extractionPassesTheMeetingsProjectStudentsToTheExtractor() {
        givenExtractedTasks();

        service.extract(MEETING_ID);

        TaskExtractionInput input = capturedExtractionInput();
        assertThat(input.students()).containsExactly(
            new StudentCandidate(ANERUTHAN_ID, "Aneruthan"),
            new StudentCandidate(GAYATHRI_ID, "Gayathri"));
        verify(projectMemberRepository, never()).findByProjectId(OTHER_PROJECT_ID);
    }

    @Test
    void extractionPassesTheMeetingsProjectMilestonesAndNotes() {
        givenExtractedTasks();

        service.extract(MEETING_ID);

        TaskExtractionInput input = capturedExtractionInput();
        assertThat(input.milestones()).containsExactly(
            new MilestoneCandidate(BASELINE_MILESTONE_ID, "Baseline evaluation"),
            new MilestoneCandidate(DATASET_MILESTONE_ID, "Dataset preparation"));
        assertThat(input.meetingTitle()).isEqualTo("Weekly sync");
        assertThat(input.meetingDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(input.notes()).isEqualTo("ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");
        verify(milestoneRepository, never()).findByProjectId(OTHER_PROJECT_ID);
    }

    @Test
    void extractionClearsAssigneeIdsThatAreNotStudentsOfTheProject() {
        givenExtractedTasks(
            extracted("Outsider task", OTHER_PROJECT_STUDENT_ID, null),
            extracted("Professor task", PROFESSOR_ID, null),
            extracted("Unknown user task", 999L, null),
            extracted("Valid task", GAYATHRI_ID, null));

        List<TaskSuggestion> suggestions = service.extract(MEETING_ID);

        assertThat(suggestions)
            .extracting(TaskSuggestion::getTitle, TaskSuggestion::getAssigneeId)
            .containsExactly(
                tuple("Outsider task", null),
                tuple("Professor task", null),
                tuple("Unknown user task", null),
                tuple("Valid task", GAYATHRI_ID));
    }

    @Test
    void extractionClearsMilestoneIdsThatDoNotBelongToTheProject() {
        givenExtractedTasks(
            extracted("Other project milestone", null, OTHER_PROJECT_MILESTONE_ID),
            extracted("Unknown milestone", null, 999L),
            extracted("Valid milestone", null, DATASET_MILESTONE_ID));

        List<TaskSuggestion> suggestions = service.extract(MEETING_ID);

        assertThat(suggestions)
            .extracting(TaskSuggestion::getMilestoneId)
            .containsExactly(null, null, DATASET_MILESTONE_ID);
    }

    @Test
    void extractionClearsOutOfRangeConfidence() {
        givenExtractedTasks(
            extractedWithConfidence("Negative", "-0.10"),
            extractedWithConfidence("Too high", "1.50"),
            extractedWithConfidence("Lower bound", "0"),
            extractedWithConfidence("Upper bound", "1"),
            extractedWithConfidence("Missing", null));

        List<TaskSuggestion> suggestions = service.extract(MEETING_ID);

        assertThat(suggestions).extracting(TaskSuggestion::getConfidence).containsExactly(
            null,
            null,
            new BigDecimal("0.00"),
            new BigDecimal("1.00"),
            null);
    }

    @Test
    void extractionSkipsBlankTitlesAndTruncatesOverlongText() {
        String longText = "x".repeat(300);
        List<ExtractedTask> tasks = new ArrayList<>();
        tasks.add(null);
        tasks.add(extracted("   ", ANERUTHAN_ID, null));
        tasks.add(new ExtractedTask(longText, longText, longText, null, null, null, longText, null));
        when(taskExtractor.extract(any())).thenReturn(tasks);

        List<TaskSuggestion> suggestions = service.extract(MEETING_ID);

        assertThat(suggestions).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.getTitle()).hasSize(TaskSuggestionService.MAX_TEXT_LENGTH);
            assertThat(suggestion.getDescription()).hasSize(TaskSuggestionService.MAX_TEXT_LENGTH);
            assertThat(suggestion.getAssigneeName()).hasSize(TaskSuggestionService.MAX_TEXT_LENGTH);
            assertThat(suggestion.getSourceExcerpt()).hasSize(300);
        });
    }

    @Test
    void reExtractionDeletesOnlyPendingSuggestionsBeforeSavingNewOnes() {
        givenExtractedTasks(extracted("Run Qwen baseline", ANERUTHAN_ID, null));

        service.extract(MEETING_ID);

        InOrder order = inOrder(taskExtractor, taskSuggestionRepository);
        order.verify(taskExtractor).extract(any());
        order.verify(taskSuggestionRepository)
            .deleteByMeetingIdAndStatus(MEETING_ID, TaskSuggestionStatus.PENDING);
        order.verify(taskSuggestionRepository).saveAll(anyList());
        verify(taskSuggestionRepository, never())
            .deleteByMeetingIdAndStatus(any(), eq(TaskSuggestionStatus.ACCEPTED));
        verify(taskSuggestionRepository, never())
            .deleteByMeetingIdAndStatus(any(), eq(TaskSuggestionStatus.REJECTED));
        verify(taskSuggestionRepository, never()).deleteAll();
        verify(taskSuggestionRepository, never()).deleteAll(any());
    }

    @Test
    void extractionFailureLeavesExistingSuggestionsUntouched() {
        when(taskExtractor.extract(any())).thenThrow(new IllegalStateException("AI unavailable"));

        assertThatThrownBy(() -> service.extract(MEETING_ID)).isInstanceOf(IllegalStateException.class);

        verify(taskSuggestionRepository, never()).deleteByMeetingIdAndStatus(any(), any());
        verify(taskSuggestionRepository, never()).saveAll(anyList());
    }

    @Test
    void extractionForMissingMeetingIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.extract(999L));
        verifyNoInteractions(taskExtractor);
    }

    @Test
    void extractionForMeetingWithoutProjectIsBadRequest() {
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting(null)));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.extract(MEETING_ID));
        verifyNoInteractions(taskExtractor);
    }

    // ----- Listing and editing -----

    @Test
    void listsSuggestionsForTheMeetingInIdOrder() {
        TaskSuggestion second = pendingSuggestion(42L);
        TaskSuggestion first = pendingSuggestion(41L);
        when(taskSuggestionRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(second, first));

        assertThat(service.listByMeeting(MEETING_ID)).containsExactly(first, second);
    }

    @Test
    void listingForMissingMeetingIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.listByMeeting(999L));
    }

    @Test
    void updatesPendingSuggestionWithoutCreatingTask() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        TaskSuggestion updated = service.update(SUGGESTION_ID, new AcceptTaskSuggestionRequest(
            "  Run Qwen baseline on dev split ",
            " Record accuracy ",
            GAYATHRI_ID,
            DATASET_MILESTONE_ID,
            LocalDate.of(2026, 10, 10)));

        assertThat(updated.getStatus()).isEqualTo(TaskSuggestionStatus.PENDING);
        assertThat(updated.getTitle()).isEqualTo("Run Qwen baseline on dev split");
        assertThat(updated.getDescription()).isEqualTo("Record accuracy");
        assertThat(updated.getAssigneeId()).isEqualTo(GAYATHRI_ID);
        assertThat(updated.getMilestoneId()).isEqualTo(DATASET_MILESTONE_ID);
        assertThat(updated.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        verify(taskSuggestionRepository).save(updated);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void cannotUpdateAcceptedOrRejectedSuggestions() {
        givenSuggestion(suggestionWithStatus(41L, TaskSuggestionStatus.ACCEPTED));
        givenSuggestion(suggestionWithStatus(42L, TaskSuggestionStatus.REJECTED));
        AcceptTaskSuggestionRequest edit = titleOnly("Changed");

        assertStatus(HttpStatus.CONFLICT, () -> service.update(41L, edit));
        assertStatus(HttpStatus.CONFLICT, () -> service.update(42L, edit));
        verify(taskSuggestionRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidValuesAndLeavesSuggestionUnchanged() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        givenSuggestion(suggestion);

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.update(SUGGESTION_ID,
            new AcceptTaskSuggestionRequest("New title", null, OTHER_PROJECT_STUDENT_ID, null, null)));

        assertThat(suggestion.getTitle()).isEqualTo("Run Qwen baseline");
        assertThat(suggestion.getAssigneeId()).isEqualTo(ANERUTHAN_ID);
        verify(taskSuggestionRepository, never()).save(any());
    }

    // ----- Accept -----

    @Test
    void acceptingCreatesExactlyOneTodoTaskWithTransferredFields() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        Task created = service.accept(SUGGESTION_ID, null);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        Task task = taskCaptor.getValue();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getTitle()).isEqualTo("Run Qwen baseline");
        assertThat(task.getDescription()).isEqualTo("Evaluate on the dev split");
        assertThat(task.getAssigneeId()).isEqualTo(ANERUTHAN_ID);
        assertThat(task.getMilestoneId()).isEqualTo(BASELINE_MILESTONE_ID);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(task.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(task.getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(task.getTaskSuggestionId()).isEqualTo(SUGGESTION_ID);
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isNotNull();
        assertThat(created).isSameAs(task);
    }

    @Test
    void acceptingMarksSuggestionAcceptedAndStoresTaskId() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        givenSuggestion(suggestion);

        service.accept(SUGGESTION_ID, null);

        assertThat(suggestion.getStatus()).isEqualTo(TaskSuggestionStatus.ACCEPTED);
        assertThat(suggestion.getAcceptedTaskId()).isEqualTo(CREATED_TASK_ID);
        verify(taskSuggestionRepository).save(suggestion);
    }

    @Test
    void acceptingWithEditedValuesUsesThoseValues() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        givenSuggestion(suggestion);

        Task task = service.accept(SUGGESTION_ID, new AcceptTaskSuggestionRequest(
            "  Run Qwen baseline on dev split ",
            "  Record accuracy and F1 ",
            GAYATHRI_ID,
            DATASET_MILESTONE_ID,
            LocalDate.of(2026, 10, 10)));

        assertThat(task.getTitle()).isEqualTo("Run Qwen baseline on dev split");
        assertThat(task.getDescription()).isEqualTo("Record accuracy and F1");
        assertThat(task.getAssigneeId()).isEqualTo(GAYATHRI_ID);
        assertThat(task.getMilestoneId()).isEqualTo(DATASET_MILESTONE_ID);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(suggestion.getTitle()).isEqualTo("Run Qwen baseline on dev split");
    }

    @Test
    void acceptingKeepsSuggestionValuesForFieldsNotEdited() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        Task task = service.accept(SUGGESTION_ID, titleOnly("Run Qwen baseline again"));

        assertThat(task.getTitle()).isEqualTo("Run Qwen baseline again");
        assertThat(task.getAssigneeId()).isEqualTo(ANERUTHAN_ID);
        assertThat(task.getMilestoneId()).isEqualTo(BASELINE_MILESTONE_ID);
    }

    @Test
    void acceptingWithBlankOrOverlongTitleIsBadRequest() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, titleOnly("   ")));
        assertStatus(HttpStatus.BAD_REQUEST,
            () -> service.accept(SUGGESTION_ID, titleOnly("x".repeat(256))));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID,
            new AcceptTaskSuggestionRequest(null, "x".repeat(256), null, null, null)));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void acceptingStoredBlankTitleIsBadRequest() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        suggestion.setTitle(null);
        givenSuggestion(suggestion);

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, null));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void acceptingWithInvalidAssigneeIsBadRequest() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, assigneeOnly(PROFESSOR_ID)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, assigneeOnly(999L)));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void acceptingWithInvalidMilestoneIsBadRequest() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, milestoneOnly(999L)));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void acceptingWithStoredInvalidConfidenceIsBadRequest() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        suggestion.setConfidence(new BigDecimal("1.50"));
        givenSuggestion(suggestion);

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(SUGGESTION_ID, null));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void acceptingAlreadyAcceptedOrRejectedSuggestionIsConflict() {
        givenSuggestion(suggestionWithStatus(41L, TaskSuggestionStatus.ACCEPTED));
        givenSuggestion(suggestionWithStatus(42L, TaskSuggestionStatus.REJECTED));

        assertStatus(HttpStatus.CONFLICT, () -> service.accept(41L, null));
        assertStatus(HttpStatus.CONFLICT, () -> service.accept(42L, null));
        verifyNoInteractions(taskRepository);
        verify(taskSuggestionRepository, never()).save(any());
    }

    @Test
    void acceptingMissingSuggestionIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.accept(999L, null));
        verifyNoInteractions(taskRepository);
    }

    // ----- Reject -----

    @Test
    void rejectingMarksSuggestionRejectedWithoutCreatingTask() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        givenSuggestion(suggestion);

        TaskSuggestion rejected = service.reject(SUGGESTION_ID);

        assertThat(rejected.getStatus()).isEqualTo(TaskSuggestionStatus.REJECTED);
        assertThat(rejected.getAcceptedTaskId()).isNull();
        verify(taskSuggestionRepository).save(suggestion);
        verify(taskSuggestionRepository, never()).delete(any());
        verify(taskSuggestionRepository, never()).deleteById(any());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void rejectingAlreadyProcessedSuggestionIsConflict() {
        givenSuggestion(suggestionWithStatus(41L, TaskSuggestionStatus.ACCEPTED));
        givenSuggestion(suggestionWithStatus(42L, TaskSuggestionStatus.REJECTED));

        assertStatus(HttpStatus.CONFLICT, () -> service.reject(41L));
        assertStatus(HttpStatus.CONFLICT, () -> service.reject(42L));
        verify(taskSuggestionRepository, never()).save(any());
    }

    @Test
    void rejectingMissingSuggestionIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.reject(999L));
    }

    // ----- Project isolation -----

    @Test
    void cannotAssignStudentFromAnotherProject() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        assertStatus(HttpStatus.BAD_REQUEST,
            () -> service.accept(SUGGESTION_ID, assigneeOnly(OTHER_PROJECT_STUDENT_ID)));
        assertStatus(HttpStatus.BAD_REQUEST,
            () -> service.update(SUGGESTION_ID, assigneeOnly(OTHER_PROJECT_STUDENT_ID)));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void cannotAssignMilestoneFromAnotherProject() {
        givenSuggestion(pendingSuggestion(SUGGESTION_ID));

        assertStatus(HttpStatus.BAD_REQUEST,
            () -> service.accept(SUGGESTION_ID, milestoneOnly(OTHER_PROJECT_MILESTONE_ID)));
        assertStatus(HttpStatus.BAD_REQUEST,
            () -> service.update(SUGGESTION_ID, milestoneOnly(OTHER_PROJECT_MILESTONE_ID)));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void storedCrossProjectIdsAreRejectedOnAccept() {
        TaskSuggestion wrongStudent = pendingSuggestion(41L);
        wrongStudent.setAssigneeId(OTHER_PROJECT_STUDENT_ID);
        TaskSuggestion wrongMilestone = pendingSuggestion(42L);
        wrongMilestone.setMilestoneId(OTHER_PROJECT_MILESTONE_ID);
        givenSuggestion(wrongStudent);
        givenSuggestion(wrongMilestone);

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(41L, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.accept(42L, null));
        verifyNoInteractions(taskRepository);
    }

    // ----- Helpers -----

    private void givenExtractedTasks(ExtractedTask... tasks) {
        when(taskExtractor.extract(any())).thenReturn(List.of(tasks));
    }

    private TaskExtractionInput capturedExtractionInput() {
        ArgumentCaptor<TaskExtractionInput> captor = ArgumentCaptor.forClass(TaskExtractionInput.class);
        verify(taskExtractor).extract(captor.capture());
        return captor.getValue();
    }

    private void givenSuggestion(TaskSuggestion suggestion) {
        when(taskSuggestionRepository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));
    }

    private void assertStatus(HttpStatus expected, Executable call) {
        assertThatThrownBy(call::execute)
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(expected));
    }

    private static ExtractedTask extracted(String title, Long assigneeId, Long milestoneId) {
        return new ExtractedTask(title, null, null, assigneeId, milestoneId, null, null, null);
    }

    private static ExtractedTask extractedWithConfidence(String title, String confidence) {
        return new ExtractedTask(title, null, null, null, null, null, null,
            confidence == null ? null : new BigDecimal(confidence));
    }

    private static AcceptTaskSuggestionRequest titleOnly(String title) {
        return new AcceptTaskSuggestionRequest(title, null, null, null, null);
    }

    private static AcceptTaskSuggestionRequest assigneeOnly(Long assigneeId) {
        return new AcceptTaskSuggestionRequest(null, null, assigneeId, null, null);
    }

    private static AcceptTaskSuggestionRequest milestoneOnly(Long milestoneId) {
        return new AcceptTaskSuggestionRequest(null, null, null, milestoneId, null);
    }

    private static TaskSuggestion pendingSuggestion(Long id) {
        return suggestionWithStatus(id, TaskSuggestionStatus.PENDING);
    }

    private static TaskSuggestion suggestionWithStatus(Long id, TaskSuggestionStatus status) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setId(id);
        suggestion.setMeetingId(MEETING_ID);
        suggestion.setProjectId(PROJECT_ID);
        suggestion.setTitle("Run Qwen baseline");
        suggestion.setDescription("Evaluate on the dev split");
        suggestion.setAssigneeName("Aneruthan");
        suggestion.setAssigneeId(ANERUTHAN_ID);
        suggestion.setMilestoneId(BASELINE_MILESTONE_ID);
        suggestion.setDueDate(LocalDate.of(2026, 10, 5));
        suggestion.setConfidence(new BigDecimal("0.90"));
        suggestion.setStatus(status);
        return suggestion;
    }

    private static Meeting meeting(Long projectId) {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(projectId);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 2));
        meeting.setNotes("ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");
        return meeting;
    }

    private static ProjectMember member(Long projectId, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private static Milestone milestone(Long id, Long projectId, String title) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProjectId(projectId);
        milestone.setTitle(title);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setWeight(new BigDecimal("50"));
        return milestone;
    }

    private static User user(Long id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    @Test
    void acceptLinkedSuggestionPropagatesResearchDecisionAndExperiment() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        when(taskSuggestionRepository.findById(SUGGESTION_ID)).thenReturn(Optional.of(suggestion));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(CREATED_TASK_ID);
            return t;
        });

        ResearchDecision decision = new ResearchDecision();
        decision.setId(101L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(101L)).thenReturn(Optional.of(decision));

        Experiment experiment = new Experiment();
        experiment.setId(201L);
        experiment.setProjectId(PROJECT_ID);
        when(experimentRepository.findById(201L)).thenReturn(Optional.of(experiment));

        AcceptTaskSuggestionRequest request = new AcceptTaskSuggestionRequest(
            "Linked Task",
            "Task with research links",
            ANERUTHAN_ID,
            BASELINE_MILESTONE_ID,
            LocalDate.of(2026, 11, 15),
            101L,
            201L
        );

        Task created = service.accept(SUGGESTION_ID, request);

        assertThat(created.getResearchDecisionId()).isEqualTo(101L);
        assertThat(created.getExperimentId()).isEqualTo(201L);
        assertThat(created.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(created.getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(created.getTaskSuggestionId()).isEqualTo(SUGGESTION_ID);

        assertThat(suggestion.getResearchDecisionId()).isEqualTo(101L);
        assertThat(suggestion.getExperimentId()).isEqualTo(201L);
        assertThat(suggestion.getStatus()).isEqualTo(TaskSuggestionStatus.ACCEPTED);
        assertThat(suggestion.getAcceptedTaskId()).isEqualTo(CREATED_TASK_ID);
    }

    @Test
    void crossProjectDecisionLinkIsRejected() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        when(taskSuggestionRepository.findById(SUGGESTION_ID)).thenReturn(Optional.of(suggestion));

        ResearchDecision otherProjectDecision = new ResearchDecision();
        otherProjectDecision.setId(999L);
        otherProjectDecision.setProjectId(OTHER_PROJECT_ID);
        when(researchDecisionRepository.findById(999L)).thenReturn(Optional.of(otherProjectDecision));

        AcceptTaskSuggestionRequest request = new AcceptTaskSuggestionRequest(
            "Task title",
            null,
            ANERUTHAN_ID,
            BASELINE_MILESTONE_ID,
            null,
            999L,
            null
        );

        assertThatThrownBy(() -> service.accept(SUGGESTION_ID, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Research decision must belong to the meeting's project");
    }

    @Test
    void crossProjectExperimentLinkIsRejected() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        when(taskSuggestionRepository.findById(SUGGESTION_ID)).thenReturn(Optional.of(suggestion));

        Experiment otherProjectExperiment = new Experiment();
        otherProjectExperiment.setId(888L);
        otherProjectExperiment.setProjectId(OTHER_PROJECT_ID);
        when(experimentRepository.findById(888L)).thenReturn(Optional.of(otherProjectExperiment));

        AcceptTaskSuggestionRequest request = new AcceptTaskSuggestionRequest(
            "Task title",
            null,
            ANERUTHAN_ID,
            BASELINE_MILESTONE_ID,
            null,
            null,
            888L
        );

        assertThatThrownBy(() -> service.accept(SUGGESTION_ID, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Experiment must belong to the meeting's project");
    }

    @Test
    void existingSuggestionWithoutResearchLinksContinuesWorking() {
        TaskSuggestion suggestion = pendingSuggestion(SUGGESTION_ID);
        when(taskSuggestionRepository.findById(SUGGESTION_ID)).thenReturn(Optional.of(suggestion));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(CREATED_TASK_ID);
            return t;
        });

        AcceptTaskSuggestionRequest request = new AcceptTaskSuggestionRequest(
            "Plain task",
            "No research links",
            ANERUTHAN_ID,
            BASELINE_MILESTONE_ID,
            LocalDate.of(2026, 11, 20)
        );

        Task created = service.accept(SUGGESTION_ID, request);

        assertThat(created.getResearchDecisionId()).isNull();
        assertThat(created.getExperimentId()).isNull();
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
    }
}