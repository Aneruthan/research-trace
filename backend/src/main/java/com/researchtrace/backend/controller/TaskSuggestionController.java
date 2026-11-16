package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.service.TaskSuggestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/meetings/{meetingId}/task-suggestions")
public class TaskSuggestionController {

    private final TaskSuggestionService taskSuggestionService;

    public TaskSuggestionController(TaskSuggestionService taskSuggestionService) {
        this.taskSuggestionService = taskSuggestionService;
    }

    @PostMapping("/extract")
    public List<TaskSuggestion> extractTaskSuggestions(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return taskSuggestionService.extract(projectId, meetingId);
    }

    @GetMapping
    public List<TaskSuggestion> getTaskSuggestionsByMeeting(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return taskSuggestionService.listByMeeting(projectId, meetingId);
    }

    @PutMapping("/{suggestionId}")
    public TaskSuggestion updateTaskSuggestion(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @PathVariable Long suggestionId,
            @RequestBody AcceptTaskSuggestionRequest request) {
        return taskSuggestionService.update(projectId, meetingId, suggestionId, request);
    }

    @PostMapping("/{suggestionId}/accept")
    public Task acceptTaskSuggestion(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @PathVariable Long suggestionId,
            @RequestBody(required = false) AcceptTaskSuggestionRequest request) {
        return taskSuggestionService.accept(projectId, meetingId, suggestionId, request);
    }

    @PostMapping("/{suggestionId}/reject")
    public TaskSuggestion rejectTaskSuggestion(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @PathVariable Long suggestionId) {
        return taskSuggestionService.reject(projectId, meetingId, suggestionId);
    }
}
