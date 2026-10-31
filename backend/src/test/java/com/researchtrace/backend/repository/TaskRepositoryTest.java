package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    private static final Long MILESTONE_ID = 11L;
    private static final Long OTHER_MILESTONE_ID = 12L;
    private static final Long STUDENT_ID = 7L;
    private static final Long OTHER_STUDENT_ID = 8L;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void countsTotalTasksForOneMilestone() {
        taskRepository.save(task(MILESTONE_ID, TaskStatus.COMPLETED, "Completed evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.TODO, "Todo evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.IN_PROGRESS, "In-progress evaluation"));
        taskRepository.save(task(OTHER_MILESTONE_ID, TaskStatus.COMPLETED, "Other milestone task"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByMilestoneId(MILESTONE_ID)).isEqualTo(3);
    }

    @Test
    void countsOnlyCompletedTasksForAMilestone() {
        taskRepository.save(task(MILESTONE_ID, TaskStatus.COMPLETED, "Completed evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.TODO, "Todo evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.IN_PROGRESS, "In-progress evaluation"));
        taskRepository.save(task(OTHER_MILESTONE_ID, TaskStatus.COMPLETED, "Other milestone task"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByMilestoneIdAndStatus(MILESTONE_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
    }

    @Test
    void doesNotCountTodoOrInProgressAsCompleted() {
        taskRepository.save(task(MILESTONE_ID, TaskStatus.TODO, "Todo evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.IN_PROGRESS, "In-progress evaluation"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByMilestoneId(MILESTONE_ID)).isEqualTo(2);
        assertThat(taskRepository.countByMilestoneIdAndStatus(MILESTONE_ID, TaskStatus.COMPLETED))
            .isEqualTo(0);
    }

    @Test
    void doesNotIncludeTasksFromAnotherMilestone() {
        taskRepository.save(task(MILESTONE_ID, TaskStatus.COMPLETED, "Completed evaluation"));
        taskRepository.save(task(MILESTONE_ID, TaskStatus.TODO, "Todo evaluation"));
        taskRepository.save(task(OTHER_MILESTONE_ID, TaskStatus.COMPLETED, "Other completed task"));
        taskRepository.save(task(OTHER_MILESTONE_ID, TaskStatus.IN_PROGRESS, "Other in-progress task"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByMilestoneId(MILESTONE_ID)).isEqualTo(2);
        assertThat(taskRepository.countByMilestoneIdAndStatus(MILESTONE_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
        assertThat(taskRepository.countByMilestoneId(OTHER_MILESTONE_ID)).isEqualTo(2);
        assertThat(taskRepository.countByMilestoneIdAndStatus(OTHER_MILESTONE_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
    }

    @Test
    void countsOnlyTasksAssignedToOneStudent() {
        taskRepository.save(task(MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED, "Student completed"));
        taskRepository.save(task(MILESTONE_ID, STUDENT_ID, TaskStatus.TODO, "Student todo"));
        taskRepository.save(task(MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.COMPLETED, "Other completed"));
        taskRepository.save(task(MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.COMPLETED, "Other completed 2"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByAssigneeId(STUDENT_ID)).isEqualTo(2);
        assertThat(taskRepository.countByAssigneeIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
    }

    @Test
    void doesNotCountStudentTodoOrInProgressAsCompleted() {
        taskRepository.save(task(MILESTONE_ID, STUDENT_ID, TaskStatus.TODO, "Student todo"));
        taskRepository.save(task(MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS, "Student in progress"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByAssigneeId(STUDENT_ID)).isEqualTo(2);
        assertThat(taskRepository.countByAssigneeIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED))
            .isEqualTo(0);
    }

    @Test
    void returnsZeroCountsForStudentWithNoTasks() {
        taskRepository.save(task(MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.COMPLETED, "Other completed"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByAssigneeId(STUDENT_ID)).isEqualTo(0);
        assertThat(taskRepository.countByAssigneeIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED))
            .isEqualTo(0);
    }

    @Test
    void keepsStudentAndMilestoneCountsIndependent() {
        taskRepository.save(task(MILESTONE_ID, STUDENT_ID, TaskStatus.COMPLETED, "Student on milestone"));
        taskRepository.save(task(OTHER_MILESTONE_ID, STUDENT_ID, TaskStatus.IN_PROGRESS, "Student on other"));
        taskRepository.save(task(MILESTONE_ID, OTHER_STUDENT_ID, TaskStatus.TODO, "Other on milestone"));
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.countByAssigneeId(STUDENT_ID)).isEqualTo(2);
        assertThat(taskRepository.countByAssigneeIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
        assertThat(taskRepository.countByAssigneeId(OTHER_STUDENT_ID)).isEqualTo(1);
        assertThat(taskRepository.countByAssigneeIdAndStatus(OTHER_STUDENT_ID, TaskStatus.COMPLETED))
            .isEqualTo(0);
        assertThat(taskRepository.countByMilestoneId(MILESTONE_ID)).isEqualTo(2);
        assertThat(taskRepository.countByMilestoneIdAndStatus(MILESTONE_ID, TaskStatus.COMPLETED))
            .isEqualTo(1);
    }

    @Test
    void findsTasksByProjectId() {
        Task p1Task = task(MILESTONE_ID, TaskStatus.TODO, "Project 1 Task");
        p1Task.setProjectId(100L);
        taskRepository.save(p1Task);

        Task p2Task = task(MILESTONE_ID, TaskStatus.TODO, "Project 2 Task");
        p2Task.setProjectId(200L);
        taskRepository.save(p2Task);

        entityManager.flush();
        entityManager.clear();

        List<Task> p1Tasks = taskRepository.findByProjectId(100L);
        assertThat(p1Tasks).hasSize(1);
        assertThat(p1Tasks.get(0).getTitle()).isEqualTo("Project 1 Task");
        assertThat(p1Tasks.get(0).getCreatedAt()).isNotNull();
        assertThat(p1Tasks.get(0).getUpdatedAt()).isNotNull();
    }

    @Test
    void findsTasksByMeetingIdAndSuggestionId() {
        Task task = task(null, TaskStatus.TODO, "Meeting Task");
        task.setMeetingId(55L);
        task.setTaskSuggestionId(77L);
        taskRepository.save(task);

        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.findByMeetingId(55L)).hasSize(1);
        assertThat(taskRepository.findByTaskSuggestionId(77L)).hasSize(1);
    }

    private Task task(Long milestoneId, TaskStatus status, String title) {
        return task(milestoneId, STUDENT_ID, status, title);
    }

    private Task task(Long milestoneId, Long assigneeId, TaskStatus status, String title) {
        Task task = new Task();
        task.setMilestoneId(milestoneId);
        task.setAssigneeId(assigneeId);
        task.setTitle(title);
        task.setDescription("Task used by the progress count tests");
        task.setStatus(status);
        task.setDueDate(LocalDate.of(2026, 9, 25));
        return task;
    }

    @Test
    void findsTasksByResearchDecisionAndExperiment() {
        Task task = task(null, TaskStatus.TODO, "Research Linked Task");
        task.setProjectId(10L);
        task.setResearchDecisionId(101L);
        task.setExperimentId(201L);
        taskRepository.save(task);

        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.findByResearchDecisionId(101L)).hasSize(1);
        assertThat(taskRepository.findByExperimentId(201L)).hasSize(1);
        assertThat(taskRepository.findByProjectIdAndResearchDecisionId(10L, 101L)).hasSize(1);
        assertThat(taskRepository.findByProjectIdAndExperimentId(10L, 201L)).hasSize(1);

        assertThat(taskRepository.findByProjectIdAndResearchDecisionId(99L, 101L)).isEmpty();
        assertThat(taskRepository.findByProjectIdAndExperimentId(99L, 201L)).isEmpty();
    }
}