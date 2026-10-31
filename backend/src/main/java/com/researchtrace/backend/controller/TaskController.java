package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskService.saveTask(task);
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
    }

    @GetMapping("/{id}/history")
    public List<TaskHistory> getTaskHistory(@PathVariable Long id) {
        return taskService.getTaskHistory(id);
    }

    @GetMapping("/milestone/{milestoneId}")
    public List<Task> getTasksByMilestone(
            @PathVariable Long milestoneId) {
        return taskService.getTasksByMilestone(milestoneId);
    }

    @GetMapping("/assignee/{assigneeId}")
    public List<Task> getTasksByAssignee(
            @PathVariable Long assigneeId) {
        return taskService.getTasksByAssignee(assigneeId);
    }

    @GetMapping("/research-decision/{decisionId}")
    public List<Task> getTasksByResearchDecision(
            @PathVariable Long decisionId) {
        return taskService.getTasksByResearchDecision(decisionId);
    }

    @GetMapping("/experiment/{experimentId}")
    public List<Task> getTasksByExperiment(
            @PathVariable Long experimentId) {
        return taskService.getTasksByExperiment(experimentId);
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable Long id,
            @RequestBody Task task,
            @RequestParam(required = false) Long userId) {

        task.setId(id);
        return taskService.saveTask(task, userId);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
