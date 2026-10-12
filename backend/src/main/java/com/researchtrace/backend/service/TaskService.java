package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final MilestoneService milestoneService;
    private final TaskSuggestionRepository taskSuggestionRepository;

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository) {
        this.taskRepository = taskRepository;
        this.milestoneService = milestoneService;
        this.taskSuggestionRepository = taskSuggestionRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByMilestone(Long milestoneId) {
        return taskRepository.findByMilestoneId(milestoneId);
    }

    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId);
    }

    public List<Task> getTasksByProject(Long projectId) {
        Map<Long, Task> tasks = new LinkedHashMap<>();
        List<Long> milestoneIds = milestoneService.getMilestonesByProject(projectId).stream()
            .map(Milestone::getId)
            .filter(Objects::nonNull)
            .toList();
        if (!milestoneIds.isEmpty()) {
            for (Task task : taskRepository.findByMilestoneIdIn(milestoneIds)) {
                if (task.getId() != null) {
                    tasks.put(task.getId(), task);
                }
            }
        }

        List<Long> acceptedTaskIds = taskSuggestionRepository
            .findByProjectIdAndStatus(projectId, TaskSuggestionStatus.ACCEPTED).stream()
            .map(TaskSuggestion::getAcceptedTaskId)
            .filter(Objects::nonNull)
            .filter(id -> !tasks.containsKey(id))
            .distinct()
            .toList();
        if (!acceptedTaskIds.isEmpty()) {
            taskRepository.findAllById(acceptedTaskIds)
                .forEach(task -> tasks.put(task.getId(), task));
        }
        return List.copyOf(tasks.values());
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
}