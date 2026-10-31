package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskHistoryRepositoryTest {

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsHistoryForTaskInChronologicalOrder() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 1, 14, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 2, 10, 0);

        TaskHistory entry1 = new TaskHistory(100L, null, TaskStatus.TODO, 1L, t1);
        TaskHistory entry2 = new TaskHistory(100L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, 2L, t2);
        TaskHistory entry3 = new TaskHistory(100L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, 1L, t3);
        TaskHistory otherTask = new TaskHistory(200L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, 1L, t2);

        // Save in non-chronological order to test query sorting
        taskHistoryRepository.save(entry2);
        taskHistoryRepository.save(entry3);
        taskHistoryRepository.save(entry1);
        taskHistoryRepository.save(otherTask);
        entityManager.flush();
        entityManager.clear();

        List<TaskHistory> history = taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(100L);

        assertThat(history).hasSize(3);
        assertThat(history.get(0).getNewStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(history.get(1).getNewStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(history.get(2).getNewStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void returnsEmptyListWhenTaskHasNoHistory() {
        List<TaskHistory> history = taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(999L);
        assertThat(history).isEmpty();
    }
}
