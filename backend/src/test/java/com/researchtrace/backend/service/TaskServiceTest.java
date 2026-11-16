package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.TaskHistoryRepository;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskSuggestionRepository taskSuggestionRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ResearchEventRepository researchEventRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository, researchDecisionRepository, experimentRepository, researchEventRepository);
    }

    @Test
    void getTaskForProjectReturnsTheTaskWhenItBelongsToTheProject() {
        Task task = task(10L, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThat(taskService.getTaskForProject(PROJECT_ID, 10L).getTitle()).isEqualTo("Task 10");
    }

    @Test
    void getTaskForProjectRejectsATaskFromAnotherProject() {
        Task task = task(10L, null);
        task.setProjectId(999L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTaskForProject(PROJECT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getTaskForProjectRejectsAMissingTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskForProject(PROJECT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void projectCannotReadAnotherProjectsTaskHistory() {
        Task foreign = task(10L, null);
        foreign.setProjectId(2L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> taskService.getTaskHistoryForProject(PROJECT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(taskHistoryRepository, never()).findByTaskIdOrderByChangedAtAscIdAsc(any());
    }

    @Test
    void projectCanReadItsOwnTaskHistory() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L, null)));
        when(taskRepository.existsById(10L)).thenReturn(true);
        TaskHistory history = new TaskHistory(10L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, null, LocalDateTime.now());
        when(taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(10L)).thenReturn(List.of(history));

        assertThat(taskService.getTaskHistoryForProject(PROJECT_ID, 10L)).containsExactly(history);
    }

    @Test
    void projectCannotListAnotherProjectsMilestoneTasks() {
        Milestone foreign = milestone(3L);
        foreign.setProjectId(2L);
        when(milestoneService.getMilestoneById(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> taskService.getTasksByMilestoneForProject(PROJECT_ID, 3L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(taskRepository, never()).findByMilestoneIdAndProjectId(any(), any());
    }

    @Test
    void projectListsOnlyItsOwnMilestoneTasks() {
        when(milestoneService.getMilestoneById(3L)).thenReturn(Optional.of(milestone(3L)));
        Task owned = task(15L, 3L);
        when(taskRepository.findByMilestoneIdAndProjectId(3L, PROJECT_ID)).thenReturn(List.of(owned));

        assertThat(taskService.getTasksByMilestoneForProject(PROJECT_ID, 3L)).containsExactly(owned);
    }

    @Test
    void assigneeTaskListStaysInsideTheProject() {
        Task owned = task(15L, null);
        owned.setAssigneeId(7L);
        when(taskRepository.findByAssigneeIdAndProjectId(7L, PROJECT_ID)).thenReturn(List.of(owned));

        assertThat(taskService.getTasksByAssigneeForProject(PROJECT_ID, 7L)).containsExactly(owned);
        verify(taskRepository, never()).findByAssigneeIdOrderByIdAsc(any());
    }

    @Test
    void projectCannotListTasksForAnotherProjectsDecisionOrExperiment() {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(55L);
        decision.setProjectId(2L);
        when(researchDecisionRepository.findById(55L)).thenReturn(Optional.of(decision));
        Experiment experiment = new Experiment();
        experiment.setId(66L);
        experiment.setProjectId(2L);
        when(experimentRepository.findById(66L)).thenReturn(Optional.of(experiment));

        assertThatThrownBy(() -> taskService.getTasksByResearchDecisionForProject(PROJECT_ID, 55L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> taskService.getTasksByExperimentForProject(PROJECT_ID, 66L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void projectCanUpdateAndDeleteItsOwnTaskButNotAnotherProjectsTask() {
        Task owned = task(10L, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(owned));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Task update = new Task();
        update.setTitle("Updated");
        update.setStatus(TaskStatus.IN_PROGRESS);

        Task saved = taskService.updateTaskForProject(PROJECT_ID, 10L, update, null);
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getTitle()).isEqualTo("Updated");

        taskService.deleteTaskForProject(PROJECT_ID, 10L);
        verify(taskRepository).deleteById(10L);

        Task foreign = task(11L, null);
        foreign.setProjectId(2L);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> taskService.updateTaskForProject(PROJECT_ID, 11L, update, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> taskService.deleteTaskForProject(PROJECT_ID, 11L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(taskRepository, never()).deleteById(11L);
    }

    @Test
    void rejectsAMeetingOrAssigneeFromAnotherProject() {
        TaskService validating = new TaskService(
            taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository,
            researchDecisionRepository, experimentRepository, researchEventRepository, null,
            meetingRepository, projectMemberRepository);
        Task owned = task(10L, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(owned));
        Meeting otherMeeting = new Meeting();
        otherMeeting.setId(4L);
        otherMeeting.setProjectId(2L);
        when(meetingRepository.findById(4L)).thenReturn(Optional.of(otherMeeting));
        ProjectMember member = new ProjectMember();
        member.setUserId(8L);
        when(projectMemberRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(member));

        Task withForeignMeeting = new Task();
        withForeignMeeting.setTitle("Task 10");
        withForeignMeeting.setStatus(TaskStatus.TODO);
        withForeignMeeting.setMeetingId(4L);
        assertThatThrownBy(() -> validating.updateTaskForProject(PROJECT_ID, 10L, withForeignMeeting, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting must belong to the task's project");

        Task withForeignAssignee = new Task();
        withForeignAssignee.setTitle("Task 10");
        withForeignAssignee.setStatus(TaskStatus.TODO);
        withForeignAssignee.setAssigneeId(99L);
        assertThatThrownBy(() -> validating.updateTaskForProject(PROJECT_ID, 10L, withForeignAssignee, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Assignee must be a student member of the task's project");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createForProjectKeepsTheRequestedProjectAndRejectsABlankTitle() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Task input = new Task();
        input.setTitle("  Record the baseline  ");
        input.setProjectId(999L);

        Task created = taskService.createForProject(PROJECT_ID, input);

        assertThat(created.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(created.getTitle()).isEqualTo("Record the baseline");
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.getId()).isNull();

        Task blank = new Task();
        blank.setTitle(" ");
        assertThatThrownBy(() -> taskService.createForProject(PROJECT_ID, blank))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void returnsTasksOnTheProjectsMilestones() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone(11L), milestone(12L)));
        Task first = task(500L, 11L);
        Task second = task(501L, 12L);
        when(taskRepository.findByMilestoneIdIn(List.of(11L, 12L))).thenReturn(List.of(first, second));
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of());

        assertThat(taskService.getTasksByProject(PROJECT_ID)).containsExactly(first, second);
    }

    @Test
    void includesAcceptedTasksThatHaveNoMilestone() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone(11L)));
        Task onMilestone = task(500L, 11L);
        Task acceptedWithoutMilestone = task(501L, null);
        when(taskRepository.findByMilestoneIdIn(List.of(11L))).thenReturn(List.of(onMilestone));
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of(acceptedSuggestion(500L), acceptedSuggestion(501L)));
        when(taskRepository.findAllById(List.of(501L))).thenReturn(List.of(acceptedWithoutMilestone));

        assertThat(taskService.getTasksByProject(PROJECT_ID))
            .containsExactly(onMilestone, acceptedWithoutMilestone);
    }

    @Test
    void doesNotQueryMilestonesWhenTheProjectHasNone() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of());

        assertThat(taskService.getTasksByProject(PROJECT_ID)).isEmpty();
        verify(taskRepository, never()).findByMilestoneIdIn(List.of());
    }

    @Test
    void excludesTasksOwnedByAnotherProject() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone(11L)));
        Task foreignMilestoneTask = task(700L, 11L);
        foreignMilestoneTask.setProjectId(999L);
        when(taskRepository.findByMilestoneIdIn(List.of(11L))).thenReturn(List.of(foreignMilestoneTask));
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of(acceptedSuggestion(800L)));
        Task foreignAcceptedTask = task(800L, null);
        foreignAcceptedTask.setProjectId(999L);
        when(taskRepository.findAllById(List.of(800L))).thenReturn(List.of(foreignAcceptedTask));

        assertThat(taskService.getTasksByProject(PROJECT_ID)).isEmpty();
    }

    @Test
    void returnsDirectProjectTasksEvenWithoutMilestones() {
        Task directWithoutMilestone = task(600L, null);
        directWithoutMilestone.setProjectId(PROJECT_ID);
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(directWithoutMilestone));
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of());

        List<Task> tasks = taskService.getTasksByProject(PROJECT_ID);
        assertThat(tasks).containsExactly(directWithoutMilestone);
    }

    @Test
    void saveTaskPopulatesProjectIdFromMilestoneWhenNull() {
        Task newTask = new Task();
        newTask.setTitle("New manual task");
        newTask.setMilestoneId(11L);

        Milestone milestone = milestone(11L);
        when(milestoneService.getMilestoneById(11L)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.saveTask(newTask);
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void saveTaskAllowsValidMilestoneAssociationWhenProjectMatches() {
        Task task = new Task();
        task.setTitle("Valid milestone task");
        task.setProjectId(PROJECT_ID);
        task.setMilestoneId(11L);

        Milestone milestone = milestone(11L);
        when(milestoneService.getMilestoneById(11L)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.saveTask(task);
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getMilestoneId()).isEqualTo(11L);
    }

    @Test
    void saveTaskRejectsMismatchedProjectAndMilestoneOnCreate() {
        Task task = new Task();
        task.setTitle("Cross-project milestone task");
        task.setProjectId(PROJECT_ID);
        task.setMilestoneId(20L);

        Milestone otherProjectMilestone = new Milestone();
        otherProjectMilestone.setId(20L);
        otherProjectMilestone.setProjectId(999L);
        when(milestoneService.getMilestoneById(20L)).thenReturn(Optional.of(otherProjectMilestone));

        assertThatThrownBy(() -> taskService.saveTask(task))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Milestone must belong to the task's project");
    }

    @Test
    void saveTaskRejectsMismatchedMilestoneOnUpdateEvenWhenProjectIdOmitted() {
        Task existing = new Task();
        existing.setId(701L);
        existing.setTitle("Existing task");
        existing.setProjectId(PROJECT_ID);
        existing.setMilestoneId(11L);

        when(taskRepository.findById(701L)).thenReturn(Optional.of(existing));

        Milestone otherProjectMilestone = new Milestone();
        otherProjectMilestone.setId(22L);
        otherProjectMilestone.setProjectId(999L);
        when(milestoneService.getMilestoneById(22L)).thenReturn(Optional.of(otherProjectMilestone));

        Task update = new Task();
        update.setId(701L);
        update.setMilestoneId(22L);

        assertThatThrownBy(() -> taskService.saveTask(update))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Milestone must belong to the task's project");
    }

    @Test
    void saveTaskRejectsNonExistentMilestone() {
        Task task = new Task();
        task.setTitle("Missing milestone task");
        task.setProjectId(PROJECT_ID);
        task.setMilestoneId(404L);

        when(milestoneService.getMilestoneById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.saveTask(task))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Milestone not found: 404");
    }

    @Test
    void saveTaskPreservesExistingProvenanceOnUpdate() {
        LocalDateTime past = LocalDateTime.of(2026, 10, 1, 10, 0);
        Task existing = new Task();
        existing.setId(700L);
        existing.setProjectId(PROJECT_ID);
        existing.setMeetingId(4L);
        existing.setTaskSuggestionId(1L);
        existing.setCreatedAt(past);

        when(taskRepository.findById(700L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task update = new Task();
        update.setId(700L);
        update.setTitle("Updated title");
        update.setStatus(TaskStatus.COMPLETED);

        Task saved = taskService.saveTask(update);
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getMeetingId()).isEqualTo(4L);
        assertThat(saved.getTaskSuggestionId()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isEqualTo(past);
        assertThat(saved.getUpdatedAt()).isAfter(past);
    }

    @Test
    void saveTaskRecordsHistoryWhenStatusChangesFromTodoToInProgress() {
        Task existing = new Task();
        existing.setId(801L);
        existing.setStatus(TaskStatus.TODO);

        when(taskRepository.findById(801L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(801L);
        update.setStatus(TaskStatus.IN_PROGRESS);

        taskService.saveTask(update);

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();
        assertThat(history.getTaskId()).isEqualTo(801L);
        assertThat(history.getPreviousStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(history.getNewStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(history.getChangedByUserId()).isNull();
        assertThat(history.getChangedAt()).isNotNull();
    }

    @Test
    void saveTaskRecordsHistoryWhenStatusChangesFromInProgressToCompletedWithUserId() {
        Task existing = new Task();
        existing.setId(802L);
        existing.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(802L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(802L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update, 42L);

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();
        assertThat(history.getTaskId()).isEqualTo(802L);
        assertThat(history.getPreviousStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(history.getNewStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(history.getChangedByUserId()).isEqualTo(42L);
        assertThat(history.getChangedAt()).isNotNull();
    }

    @Test
    void saveTaskDoesNotRecordHistoryWhenStatusDoesNotChange() {
        Task existing = new Task();
        existing.setId(803L);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setTitle("Initial title");

        when(taskRepository.findById(803L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(803L);
        update.setStatus(TaskStatus.IN_PROGRESS);
        update.setTitle("Updated title");

        taskService.saveTask(update);

        verify(taskHistoryRepository, never()).save(any());
    }

    @Test
    void saveTaskDoesNotRecordHistoryOnNewTaskCreation() {
        Task newTask = new Task();
        newTask.setStatus(TaskStatus.TODO);
        newTask.setTitle("Brand new task");

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.saveTask(newTask);

        verify(taskHistoryRepository, never()).save(any());
    }

    @Test
    void getTaskHistoryReturnsChronologicalList() {
        Long taskId = 805L;
        when(taskRepository.existsById(taskId)).thenReturn(true);

        TaskHistory h1 = new TaskHistory(taskId, TaskStatus.TODO, TaskStatus.IN_PROGRESS, null, LocalDateTime.now().minusHours(2));
        TaskHistory h2 = new TaskHistory(taskId, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, 10L, LocalDateTime.now().minusHours(1));
        when(taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(taskId)).thenReturn(List.of(h1, h2));

        List<TaskHistory> result = taskService.getTaskHistory(taskId);

        assertThat(result).containsExactly(h1, h2);
    }

    @Test
    void getTaskHistoryThrowsNotFoundWhenTaskDoesNotExist() {
        Long missingId = 999L;
        when(taskRepository.existsById(missingId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.getTaskHistory(missingId))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Task not found: 999");

        verify(taskHistoryRepository, never()).findByTaskIdOrderByChangedAtAscIdAsc(any());
    }

    private static Milestone milestone(Long id) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProjectId(PROJECT_ID);
        return milestone;
    }

    private static Task task(Long id, Long milestoneId) {
        Task task = new Task();
        task.setId(id);
        task.setProjectId(PROJECT_ID);
        task.setMilestoneId(milestoneId);
        task.setTitle("Task " + id);
        return task;
    }

    private static TaskSuggestion acceptedSuggestion(Long taskId) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setProjectId(PROJECT_ID);
        suggestion.setStatus(TaskSuggestionStatus.ACCEPTED);
        suggestion.setAcceptedTaskId(taskId);
        return suggestion;
    }

    @Test
    void getTasksByResearchDecisionReturnsMatchingTasks() {
        Task t = new Task();
        t.setId(101L);
        t.setResearchDecisionId(55L);
        when(taskRepository.findByResearchDecisionId(55L)).thenReturn(List.of(t));

        List<Task> tasks = taskService.getTasksByResearchDecision(55L);
        assertThat(tasks).containsExactly(t);
    }

    @Test
    void getTasksByExperimentReturnsMatchingTasks() {
        Task t = new Task();
        t.setId(102L);
        t.setExperimentId(66L);
        when(taskRepository.findByExperimentId(66L)).thenReturn(List.of(t));

        List<Task> tasks = taskService.getTasksByExperiment(66L);
        assertThat(tasks).containsExactly(t);
    }

    @Test
    void saveTaskRejectsCrossProjectResearchDecision() {
        Task task = new Task();
        task.setProjectId(PROJECT_ID);
        task.setResearchDecisionId(99L);

        ResearchDecision otherProjectDecision = new ResearchDecision();
        otherProjectDecision.setId(99L);
        otherProjectDecision.setProjectId(2L);

        when(researchDecisionRepository.findById(99L)).thenReturn(Optional.of(otherProjectDecision));

        assertThatThrownBy(() -> taskService.saveTask(task))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Research decision must belong to the task's project");
    }

    @Test
    void saveTaskRejectsCrossProjectExperiment() {
        Task task = new Task();
        task.setProjectId(PROJECT_ID);
        task.setExperimentId(88L);

        Experiment otherProjectExperiment = new Experiment();
        otherProjectExperiment.setId(88L);
        otherProjectExperiment.setProjectId(2L);

        when(experimentRepository.findById(88L)).thenReturn(Optional.of(otherProjectExperiment));

        assertThatThrownBy(() -> taskService.saveTask(task))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Experiment must belong to the task's project");
    }

    @Test
    void saveTaskPreservesExistingResearchLinksOnUpdateWhenOmitted() {
        Task existing = new Task();
        existing.setId(750L);
        existing.setProjectId(PROJECT_ID);
        existing.setResearchDecisionId(55L);
        existing.setExperimentId(66L);
        existing.setStatus(TaskStatus.TODO);

        ResearchDecision decision = new ResearchDecision();
        decision.setId(55L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(55L)).thenReturn(Optional.of(decision));

        Experiment experiment = new Experiment();
        experiment.setId(66L);
        experiment.setProjectId(PROJECT_ID);
        when(experimentRepository.findById(66L)).thenReturn(Optional.of(experiment));

        when(taskRepository.findById(750L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(750L);
        update.setTitle("Updated title without touching research links");
        update.setStatus(TaskStatus.IN_PROGRESS);

        Task saved = taskService.saveTask(update);

        assertThat(saved.getResearchDecisionId()).isEqualTo(55L);
        assertThat(saved.getExperimentId()).isEqualTo(66L);
    }

    @Test
    void completingTaskLinkedToResearchDecisionCreatesResearchEvent() {
        Task existing = new Task();
        existing.setId(701L);
        existing.setTitle("Implement feature");
        existing.setDescription("Task description");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(701L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        Task update = new Task();
        update.setId(701L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update, 42L);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        ResearchEvent event = eventCaptor.getValue();
        assertThat(event.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(event.getTaskId()).isEqualTo(701L);
        assertThat(event.getEventType()).isEqualTo(ResearchEventType.TASK_COMPLETED);
        assertThat(event.getTitle()).isEqualTo("Task Completed: Implement feature");
        assertThat(event.getDescription()).isEqualTo("Task description");
        assertThat(event.getResearchDecisionId()).isEqualTo(10L);
        assertThat(event.getCreatedByUserId()).isEqualTo(42L);
    }

    @Test
    void completionEventTitleFitsTheColumnForLongTaskTitles() {
        Task existing = new Task();
        existing.setId(710L);
        existing.setTitle("x".repeat(250));
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(710L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        Task update = new Task();
        update.setId(710L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        String title = eventCaptor.getValue().getTitle();
        assertThat(title).hasSize(255);
        assertThat(title).startsWith("Task Completed: xxx");
        assertThat(title).endsWith("\u2026");
    }

    @Test
    void completingTaskLinkedToExperimentCreatesResearchEvent() {
        Task existing = new Task();
        existing.setId(702L);
        existing.setTitle("Run benchmark");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.TODO);
        existing.setExperimentId(20L);

        when(taskRepository.findById(702L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Experiment exp = new Experiment();
        exp.setId(20L);
        exp.setProjectId(PROJECT_ID);
        when(experimentRepository.findById(20L)).thenReturn(Optional.of(exp));

        Task update = new Task();
        update.setId(702L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        ResearchEvent event = eventCaptor.getValue();
        assertThat(event.getTaskId()).isEqualTo(702L);
        assertThat(event.getExperimentId()).isEqualTo(20L);
        assertThat(event.getEventType()).isEqualTo(ResearchEventType.TASK_COMPLETED);
    }

    @Test
    void completingUnlinkedTaskDoesNotCreateResearchEvent() {
        Task existing = new Task();
        existing.setId(703L);
        existing.setTitle("Generic chore");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(703L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(703L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update);

        verify(researchEventRepository, never()).save(any(ResearchEvent.class));
    }

    @Test
    void updatingAlreadyCompletedTaskDoesNotCreateDuplicateResearchEvent() {
        Task existing = new Task();
        existing.setId(704L);
        existing.setTitle("Already completed task");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.COMPLETED);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(704L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        Task update = new Task();
        update.setId(704L);
        update.setTitle("Updated description of finished task");
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update);

        verify(researchEventRepository, never()).save(any(ResearchEvent.class));
    }

    @Test
    void nonCompletedStatusChangeDoesNotCreateResearchEvent() {
        Task existing = new Task();
        existing.setId(705L);
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.TODO);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(705L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        Task update = new Task();
        update.setId(705L);
        update.setStatus(TaskStatus.IN_PROGRESS);

        taskService.saveTask(update);

        verify(researchEventRepository, never()).save(any(ResearchEvent.class));
    }

    @Test
    void completingTaskLinkedToBothDecisionAndExperimentCreatesSingleEventWithBothReferences() {
        Task existing = new Task();
        existing.setId(706L);
        existing.setTitle("Dual linked task");
        existing.setDescription("Dual linked task description");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);
        existing.setExperimentId(20L);

        when(taskRepository.findById(706L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        Experiment exp = new Experiment();
        exp.setId(20L);
        exp.setProjectId(PROJECT_ID);
        when(experimentRepository.findById(20L)).thenReturn(Optional.of(exp));

        Task update = new Task();
        update.setId(706L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update, 99L);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        ResearchEvent event = eventCaptor.getValue();
        assertThat(event.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(event.getTaskId()).isEqualTo(706L);
        assertThat(event.getResearchDecisionId()).isEqualTo(10L);
        assertThat(event.getExperimentId()).isEqualTo(20L);
        assertThat(event.getEventType()).isEqualTo(ResearchEventType.TASK_COMPLETED);
        assertThat(event.getTitle()).isEqualTo("Task Completed: Dual linked task");
        assertThat(event.getDescription()).isEqualTo("Dual linked task description");
        assertThat(event.getCreatedByUserId()).isEqualTo(99L);
    }

    @Test
    void repeatedCompletionRequestsDoNotCreateDuplicateEvents() {
        Task existing = new Task();
        existing.setId(707L);
        existing.setTitle("Repeat complete task");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(707L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        // First completion request:
        Task firstUpdate = new Task();
        firstUpdate.setId(707L);
        firstUpdate.setStatus(TaskStatus.COMPLETED);
        taskService.saveTask(firstUpdate);

        verify(researchEventRepository, org.mockito.Mockito.times(1)).save(any(ResearchEvent.class));

        // Subsequent completion request when task is now already COMPLETED in database:
        Task completedTask = new Task();
        completedTask.setId(707L);
        completedTask.setTitle("Repeat complete task");
        completedTask.setProjectId(PROJECT_ID);
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask.setResearchDecisionId(10L);
        when(taskRepository.findById(707L)).thenReturn(Optional.of(completedTask));

        Task secondUpdate = new Task();
        secondUpdate.setId(707L);
        secondUpdate.setStatus(TaskStatus.COMPLETED);
        taskService.saveTask(secondUpdate);

        // Verify save was NOT called again: exactly 1 event created across repeated requests
        verify(researchEventRepository, org.mockito.Mockito.times(1)).save(any(ResearchEvent.class));
    }

    @Test
    void existingCompletionEventPreventsDuplicateWhenTransitioningToCompleted() {
        Task existing = new Task();
        existing.setId(709L);
        existing.setTitle("Task with pre-existing event");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(709L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        when(researchEventRepository.existsByTaskIdAndEventType(709L, ResearchEventType.TASK_COMPLETED)).thenReturn(true);

        Task update = new Task();
        update.setId(709L);
        update.setStatus(TaskStatus.COMPLETED);

        taskService.saveTask(update);

        verify(researchEventRepository, never()).save(any(ResearchEvent.class));
    }

    @Test
    void failureDuringResearchEventPersistenceThrowsException() {
        Task existing = new Task();
        existing.setId(708L);
        existing.setTitle("Failing event task");
        existing.setProjectId(PROJECT_ID);
        existing.setStatus(TaskStatus.IN_PROGRESS);
        existing.setResearchDecisionId(10L);

        when(taskRepository.findById(708L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        when(researchDecisionRepository.findById(10L)).thenReturn(Optional.of(decision));

        doThrow(new RuntimeException("Simulated event persistence error"))
            .when(researchEventRepository).save(any(ResearchEvent.class));

        Task update = new Task();
        update.setId(708L);
        update.setStatus(TaskStatus.COMPLETED);

        assertThatThrownBy(() -> taskService.saveTask(update, 1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Simulated event persistence error");
    }
}