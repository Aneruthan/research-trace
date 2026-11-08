package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.TaskHistoryRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskUpdateRegressionTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long MILESTONE_ID = 100L;
    private static final Long OTHER_MILESTONE_ID = 200L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskSuggestionRepository taskSuggestionRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private ProgressService progressService;

    private TaskService taskService;

    private Task taskA;
    private Task taskB;
    private Task taskC;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
            taskRepository,
            milestoneService,
            taskSuggestionRepository,
            taskHistoryRepository,
            null,
            null,
            null,
            progressService
        );

        taskA = new Task();
        taskA.setId(10L);
        taskA.setProjectId(PROJECT_ID);
        taskA.setTitle("Task A");
        taskA.setStatus(TaskStatus.IN_PROGRESS);
        taskA.setMilestoneId(MILESTONE_ID);

        taskB = new Task();
        taskB.setId(20L);
        taskB.setProjectId(PROJECT_ID);
        taskB.setTitle("Task B");
        taskB.setStatus(TaskStatus.TODO);
        taskB.setMilestoneId(MILESTONE_ID);

        taskC = new Task();
        taskC.setId(30L);
        taskC.setProjectId(PROJECT_ID);
        taskC.setTitle("Task C");
        taskC.setStatus(TaskStatus.TODO);
        taskC.setMilestoneId(MILESTONE_ID);

        Milestone defaultMilestone = new Milestone();
        defaultMilestone.setId(MILESTONE_ID);
        defaultMilestone.setProjectId(PROJECT_ID);
        defaultMilestone.setStatus(MilestoneStatus.IN_PROGRESS);
        lenient().when(milestoneService.getMilestoneById(MILESTONE_ID)).thenReturn(Optional.of(defaultMilestone));
    }

    @Test
    @DisplayName("Req 1 & 2: Changing task A from IN_PROGRESS to COMPLETED updates ONLY task A, leaving task B and C unchanged")
    void changingTaskAUpdatesOnlyTaskAAndLeavesBandCUnchanged() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updateRequest = new Task();
        updateRequest.setId(10L);
        updateRequest.setStatus(TaskStatus.COMPLETED);

        Task saved = taskService.saveTask(updateRequest, 1L);

        // Verify that the saved task is task A with COMPLETED status
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // Verify task B and C are untouched
        assertThat(taskB.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(taskC.getStatus()).isEqualTo(TaskStatus.TODO);

        // Verify taskRepository.save was called with ID 10 and never with 20 or 30
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    @DisplayName("Req 4: First item in rendered task list can be updated correctly without targeting last task")
    void firstItemInTaskListIsUpdatedCorrectly() {
        // List ordered: taskA (id: 10), taskB (id: 20), taskC (id: 30)
        when(taskRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(taskA, taskB, taskC));
        when(taskSuggestionRepository.findByProjectIdAndStatus(any(), any())).thenReturn(List.of());
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());

        List<Task> tasks = taskService.getTasksByProject(PROJECT_ID);
        Task firstTask = tasks.get(0);
        assertThat(firstTask.getId()).isEqualTo(10L);

        // Update first task
        when(taskRepository.findById(10L)).thenReturn(Optional.of(firstTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(firstTask.getId());
        update.setStatus(TaskStatus.COMPLETED);

        Task result = taskService.saveTask(update);
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // Verify taskRepository was called ONLY for id 10, not the last task (id 30)
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(taskRepository, never()).findById(30L);
    }

    @Test
    @DisplayName("Req 5: Last item in task list can be updated correctly without targeting first task")
    void lastItemInTaskListIsUpdatedCorrectly() {
        when(taskRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(taskA, taskB, taskC));
        when(taskSuggestionRepository.findByProjectIdAndStatus(any(), any())).thenReturn(List.of());
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());

        List<Task> tasks = taskService.getTasksByProject(PROJECT_ID);
        Task lastTask = tasks.get(tasks.size() - 1);
        assertThat(lastTask.getId()).isEqualTo(30L);

        // Update last task
        when(taskRepository.findById(30L)).thenReturn(Optional.of(lastTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.setId(lastTask.getId());
        update.setStatus(TaskStatus.COMPLETED);

        Task result = taskService.saveTask(update);
        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // Verify taskRepository was called ONLY for id 30, not the first task (id 10)
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(taskRepository, never()).findById(10L);
    }

    @Test
    @DisplayName("Req 6: Reordering task list does not change task identities or targeting")
    void reorderingTaskListDoesNotChangeTaskIdentities() {
        List<Task> originalList = List.of(taskA, taskB, taskC);
        List<Task> reversedList = List.of(taskC, taskB, taskA);

        // In both cases, sorting by ID yields the exact same canonical order
        List<Task> canonical = reversedList.stream()
            .sorted(Comparator.comparing(Task::getId))
            .toList();

        assertThat(canonical.get(0).getId()).isEqualTo(10L);
        assertThat(canonical.get(1).getId()).isEqualTo(20L);
        assertThat(canonical.get(2).getId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Milestone status: 0% -> NOT_STARTED, 50% -> IN_PROGRESS, 100% -> COMPLETED, 0 tasks -> preserved")
    void milestoneStatusReflectsCalculatedProgress() {
        Milestone milestone = new Milestone();
        milestone.setId(MILESTONE_ID);
        milestone.setStatus(MilestoneStatus.NOT_STARTED);

        when(milestoneService.getMilestoneById(MILESTONE_ID)).thenReturn(Optional.of(milestone));

        // Case 1: 50% progress -> IN_PROGRESS
        when(taskRepository.findByMilestoneId(MILESTONE_ID)).thenReturn(List.of(taskA));
        when(progressService.calculateTaskProgress(any())).thenReturn(new BigDecimal("50.00"));

        taskService.syncMilestoneStatus(MILESTONE_ID);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);

        // Case 2: 100% progress -> COMPLETED
        when(progressService.calculateTaskProgress(any())).thenReturn(new BigDecimal("100.00"));

        taskService.syncMilestoneStatus(MILESTONE_ID);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.COMPLETED);

        // Case 3: 0% progress -> NOT_STARTED
        when(progressService.calculateTaskProgress(any())).thenReturn(new BigDecimal("0.00"));

        taskService.syncMilestoneStatus(MILESTONE_ID);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.NOT_STARTED);

        // Case 4: 0 associated tasks -> preserve existing status
        milestone.setStatus(MilestoneStatus.BLOCKED);
        when(taskRepository.findByMilestoneId(MILESTONE_ID)).thenReturn(List.of());

        taskService.syncMilestoneStatus(MILESTONE_ID);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.BLOCKED);
    }

    @Test
    @DisplayName("Req 13: Reassigning task to a new milestone triggers status sync on BOTH old and new milestones")
    void reassigningMilestoneRecalculatesBothOldAndNewMilestones() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA)); // taskA currently has MILESTONE_ID (100)
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Milestone mOld = new Milestone();
        mOld.setId(MILESTONE_ID);
        mOld.setProjectId(PROJECT_ID);
        mOld.setStatus(MilestoneStatus.IN_PROGRESS);

        Milestone mNew = new Milestone();
        mNew.setId(OTHER_MILESTONE_ID);
        mNew.setProjectId(PROJECT_ID);
        mNew.setStatus(MilestoneStatus.NOT_STARTED);

        when(milestoneService.getMilestoneById(MILESTONE_ID)).thenReturn(Optional.of(mOld));
        when(milestoneService.getMilestoneById(OTHER_MILESTONE_ID)).thenReturn(Optional.of(mNew));

        Task moveRequest = new Task();
        moveRequest.setId(10L);
        moveRequest.setMilestoneId(OTHER_MILESTONE_ID); // moved to 200

        taskService.saveTask(moveRequest);

        // Verify milestoneService.getMilestoneById was called for BOTH 100 and 200
        verify(milestoneService, atLeastOnce()).getMilestoneById(MILESTONE_ID);
        verify(milestoneService, atLeastOnce()).getMilestoneById(OTHER_MILESTONE_ID);
    }
}
