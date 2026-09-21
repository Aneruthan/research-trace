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

    private Task task(Long milestoneId, TaskStatus status, String title) {
        Task task = new Task();
        task.setMilestoneId(milestoneId);
        task.setAssigneeId(7L);
        task.setTitle(title);
        task.setDescription("Task used by the progress count tests");
        task.setStatus(status);
        task.setDueDate(LocalDate.of(2026, 9, 25));
        return task;
    }
}
