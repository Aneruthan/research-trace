package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.service.TaskWork.Counts;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWorkTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    @Test
    void countsStatusesAndOverdueTasksWithoutDividingByZero() {
        Counts empty = TaskWork.count(List.of(), TODAY);
        assertThat(empty.total()).isZero();
        assertThat(empty.overdue()).isZero();

        Counts counts = TaskWork.count(List.of(
            task(1L, TaskStatus.COMPLETED, TODAY.minusDays(2)),
            task(2L, TaskStatus.IN_PROGRESS, TODAY.minusDays(1)),
            task(3L, TaskStatus.TODO, TODAY),
            task(4L, TaskStatus.TODO, null),
            task(1L, TaskStatus.COMPLETED, TODAY.minusDays(2))
        ), TODAY);

        assertThat(counts.total()).isEqualTo(4);
        assertThat(counts.completed()).isEqualTo(1);
        assertThat(counts.inProgress()).isEqualTo(1);
        assertThat(counts.pending()).isEqualTo(2);
        assertThat(counts.overdue()).isEqualTo(1);
    }

    @Test
    void aCompletedTaskWithAPastDueDateIsNotOverdue() {
        Task task = task(8L, TaskStatus.COMPLETED, TODAY.minusDays(3));
        assertThat(TaskWork.isOverdue(task, TODAY)).isFalse();
    }

    private static Task task(Long id, TaskStatus status, LocalDate dueDate) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(status);
        task.setDueDate(dueDate);
        return task;
    }
}
