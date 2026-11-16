package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TaskWork {

    private TaskWork() {
    }

    public static boolean isOverdue(Task task, LocalDate today) {
        return task != null
            && task.getDueDate() != null
            && today != null
            && task.getDueDate().isBefore(today)
            && task.getStatus() != TaskStatus.COMPLETED;
    }

    public static Counts count(Collection<Task> tasks, LocalDate today) {
        Map<Long, Task> unique = new LinkedHashMap<>();
        long synthetic = -1L;
        if (tasks != null) {
            for (Task task : tasks) {
                if (task == null) {
                    continue;
                }
                unique.put(task.getId() != null ? task.getId() : synthetic--, task);
            }
        }
        int completed = 0;
        int inProgress = 0;
        int pending = 0;
        int overdue = 0;
        for (Task task : unique.values()) {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                completed++;
            } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgress++;
            } else {
                pending++;
            }
            if (isOverdue(task, today)) {
                overdue++;
            }
        }
        return new Counts(unique.size(), completed, inProgress, pending, overdue);
    }

    public record Counts(int total, int completed, int inProgress, int pending, int overdue) {
    }
}
