package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.service.TaskSuggestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskSuggestionController {

    private final TaskSuggestionService taskSuggestionService;

    public TaskSuggestionController(TaskSuggestionService taskSuggestionService) {
        this.taskSuggestionService = taskSuggestionService;
    }

    @PostMapping("/meetings/{meetingId}/task-suggestions/extract")
    public List<TaskSuggestion> extractTaskSuggestions(@PathVariable Long meetingId) {
        return taskSuggestionService.extract(meetingId);
    }

    @GetMapping("/meetings/{meetingId}/task-suggestions")
    public List<TaskSuggestion> getTaskSuggestionsByMeeting(@PathVariable Long meetingId) {
        return taskSuggestionService.listByMeeting(meetingId);
    }

    @PutMapping("/task-suggestions/{id}")
    public TaskSuggestion updateTaskSuggestion(
            @PathVariable Long id,
            @RequestBody AcceptTaskSuggestionRequest request) {

        return taskSuggestionService.update(id, request);
    }

    @PostMapping("/task-suggestions/{id}/accept")
    public Task acceptTaskSuggestion(
            @PathVariable Long id,
            @RequestBody(required = false) AcceptTaskSuggestionRequest request) {

        return taskSuggestionService.accept(id, request);
    }

    @PostMapping("/task-suggestions/{id}/reject")
    public TaskSuggestion rejectTaskSuggestion(@PathVariable Long id) {
        return taskSuggestionService.reject(id);
    }
}
