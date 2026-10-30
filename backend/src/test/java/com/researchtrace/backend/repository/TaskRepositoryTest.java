package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;

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
}
