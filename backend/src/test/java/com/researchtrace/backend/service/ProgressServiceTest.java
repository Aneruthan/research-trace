package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;
    private static final Long STUDENT_ID = 7L;
    private static final Long OTHER_STUDENT_ID = 8L;
    private static final Long MILESTONE_ID = 11L;
    private static final Long OTHER_MILESTONE_ID = 12L;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProgressService progressService;

    private Task createTask(Long id, Long projectId, Long milestoneId, Long assigneeId, TaskStatus status) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(projectId);
        t.setMilestoneId(milestoneId);
        t.setAssigneeId(assigneeId);
        t.setStatus(status);
        t.setTitle("Task " + id);
        return t;
    }

    @Test
    @DisplayName("Req 7: One completed and one in-progress task produce 75.00% progress")
    void oneCompletedAndOneInProgressTaskProduces75Percent() {
        List<Task> tasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS)
        );
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(tasks);

        BigDecimal progress = progressService.getProjectProgress(PROJECT_ID);
        assertThat(progress).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Req 8: All completed tasks produce 100.00%")
    void allCompletedTasksProduce100Percent() {
        List<Task> tasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(3L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED)
        );
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(tasks);

        BigDecimal progress = progressService.getProjectProgress(PROJECT_ID);
        assertThat(progress).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Req 9: All TODO tasks produce 0.00%")
    void allTodoTasksProduceZeroPercent() {
        List<Task> tasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.TODO),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.TODO)
        );
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(tasks);

        BigDecimal progress = progressService.getProjectProgress(PROJECT_ID);
        assertThat(progress).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Req 10: Student progress is scoped to assigned tasks within project")
    void studentProgressIsScopedToAssignedTasksWithinProject() {
        List<Task> studentTasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS)
        );
        when(taskRepository.findByAssigneeIdAndProjectId(STUDENT_ID, PROJECT_ID)).thenReturn(studentTasks);

        BigDecimal progress = progressService.getStudentProgress(STUDENT_ID, PROJECT_ID);
        // (100 + 50) / 2 = 75.00%
        assertThat(progress).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Req 11: Milestone progress is scoped to milestone tasks within project")
    void milestoneProgressIsScopedToMilestoneTasksWithinProject() {
        List<Task> milestoneTasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.TODO)
        );
        when(taskRepository.findByMilestoneIdAndProjectId(MILESTONE_ID, PROJECT_ID)).thenReturn(milestoneTasks);

        BigDecimal progress = progressService.getMilestoneProgress(MILESTONE_ID, PROJECT_ID);
        // (100 + 0) / 2 = 50.00%
        assertThat(progress).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Req 12: Project progress is scoped to project tasks")
    void projectProgressIsScopedToProjectTasks() {
        List<Task> projectTasks = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(3L, PROJECT_ID, OTHER_MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.IN_PROGRESS),
            createTask(4L, PROJECT_ID, null, null, TaskStatus.TODO)
        );
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(projectTasks);

        BigDecimal progress = progressService.getProjectProgress(PROJECT_ID);
        // (100 + 100 + 50 + 0) / 4 = 250 / 4 = 62.50%
        assertThat(progress).isEqualByComparingTo("62.50");
    }

    @Test
    @DisplayName("Req 13: Moving a task recalculates old and new aggregates")
    void movingTaskRecalculatesOldAndNewMilestoneAggregates() {
        // Initially milestone 11 has task 1 (COMPLETED), milestone 12 has task 2 (IN_PROGRESS)
        List<Task> m11TasksBefore = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED)
        );
        List<Task> m12TasksBefore = List.of(
            createTask(2L, PROJECT_ID, OTHER_MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS)
        );
        when(taskRepository.findByMilestoneIdAndProjectId(MILESTONE_ID, PROJECT_ID)).thenReturn(m11TasksBefore);
        when(taskRepository.findByMilestoneIdAndProjectId(OTHER_MILESTONE_ID, PROJECT_ID)).thenReturn(m12TasksBefore);

        assertThat(progressService.getMilestoneProgress(MILESTONE_ID, PROJECT_ID)).isEqualByComparingTo("100.00");
        assertThat(progressService.getMilestoneProgress(OTHER_MILESTONE_ID, PROJECT_ID)).isEqualByComparingTo("50.00");

        // Now move task 2 from milestone 12 to milestone 11
        List<Task> m11TasksAfter = List.of(
            createTask(1L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED),
            createTask(2L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS)
        );
        List<Task> m12TasksAfter = List.of();
        when(taskRepository.findByMilestoneIdAndProjectId(MILESTONE_ID, PROJECT_ID)).thenReturn(m11TasksAfter);
        when(taskRepository.findByMilestoneIdAndProjectId(OTHER_MILESTONE_ID, PROJECT_ID)).thenReturn(m12TasksAfter);

        assertThat(progressService.getMilestoneProgress(MILESTONE_ID, PROJECT_ID)).isEqualByComparingTo("75.00");
        assertThat(progressService.getMilestoneProgress(OTHER_MILESTONE_ID, PROJECT_ID)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Deduplication: Duplicate task entries in collection do not corrupt progress")
    void duplicateTasksAreDeduplicatedById() {
        Task t1 = createTask(10L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED);
        Task t1Duplicate = createTask(10L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED);
        Task t2 = createTask(20L, PROJECT_ID, MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS);

        List<Task> tasksWithDuplicate = List.of(t1, t1Duplicate, t2);
        BigDecimal progress = progressService.calculateTaskProgress(tasksWithDuplicate);

        // Deduplicated to 2 unique tasks: 1 COMPLETED (100) + 1 IN_PROGRESS (50) => 75.00%
        assertThat(progress).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Boundedness: Progress is strictly bounded between 0.00 and 100.00")
    void progressIsBoundedBetween0And100() {
        assertThat(progressService.getProjectProgress(null)).isEqualByComparingTo("0.00");

        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());
        assertThat(progressService.getProjectProgress(PROJECT_ID)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Completing a milestone task with no project id updates project progress")
    void projectProgressIncludesMilestoneTasksWithoutProjectId() {
        Milestone milestone = new Milestone();
        milestone.setId(MILESTONE_ID);
        milestone.setProjectId(PROJECT_ID);
        Task completed = createTask(5L, null, MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED);
        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone));
        when(taskRepository.findByMilestoneId(MILESTONE_ID)).thenReturn(List.of(completed));

        assertThat(progressService.getProjectProgress(PROJECT_ID)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("No associated tasks returns 0.00%")
    void noAssociatedTasksReturnsZero() {
        when(taskRepository.findByMilestoneIdAndProjectId(MILESTONE_ID, PROJECT_ID)).thenReturn(List.of());
        when(taskRepository.findByAssigneeIdAndProjectId(STUDENT_ID, PROJECT_ID)).thenReturn(List.of());

        assertThat(progressService.getMilestoneProgress(MILESTONE_ID, PROJECT_ID)).isEqualByComparingTo("0.00");
        assertThat(progressService.getStudentProgress(STUDENT_ID, PROJECT_ID)).isEqualByComparingTo("0.00");
    }
}
