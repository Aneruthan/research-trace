package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.service.TaskService;
import org.springframework.web.bind.annotation.*;

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
            .orElseThrow(() -> new RuntimeException("Task not found"));
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

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable Long id,
            @RequestBody Task task) {

        task.setId(id);
        return taskService.saveTask(task);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}