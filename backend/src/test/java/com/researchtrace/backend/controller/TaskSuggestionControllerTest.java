package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest.ReviewedField;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.service.TaskSuggestionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskSuggestionController.class)
class TaskSuggestionControllerTest {

    private static final String EDIT_BODY = """
        {
          "title": "Run Qwen baseline on dev split",
          "description": "Record accuracy",
          "assigneeId": 8,
          "milestoneId": 12,
          "dueDate": "2026-10-10"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskSuggestionService taskSuggestionService;

    @Test
    void extractsSuggestionsForMeeting() throws Exception {
        when(taskSuggestionService.extract(1L, 21L)).thenReturn(List.of(
            suggestion(40L, TaskSuggestionStatus.PENDING),
            suggestion(41L, TaskSuggestionStatus.PENDING)));

        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/extract"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(40))
            .andExpect(jsonPath("$[0].meetingId").value(21))
            .andExpect(jsonPath("$[0].projectId").value(1))
            .andExpect(jsonPath("$[0].title").value("Run Qwen baseline"))
            .andExpect(jsonPath("$[0].assigneeName").value("Aneruthan"))
            .andExpect(jsonPath("$[0].assigneeId").value(7))
            .andExpect(jsonPath("$[0].milestoneId").value(11))
            .andExpect(jsonPath("$[0].dueDate").value("2026-10-05"))
            .andExpect(jsonPath("$[0].sourceExcerpt").value("ACTION: Aneruthan | Run Qwen baseline | 2026-10-05"))
            .andExpect(jsonPath("$[0].confidence").value(0.9))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].acceptedTaskId").doesNotExist());
    }

    @Test
    void listsSuggestionsForMeeting() throws Exception {
        when(taskSuggestionService.listByMeeting(1L, 21L)).thenReturn(List.of(
            suggestion(40L, TaskSuggestionStatus.ACCEPTED),
            suggestion(41L, TaskSuggestionStatus.PENDING)));

        mockMvc.perform(get("/api/projects/1/meetings/21/task-suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
            .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void editsPendingSuggestion() throws Exception {
        TaskSuggestion updated = suggestion(40L, TaskSuggestionStatus.PENDING);
        updated.setTitle("Run Qwen baseline on dev split");
        updated.setAssigneeId(8L);
        when(taskSuggestionService.update(eq(1L), eq(21L), eq(40L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EDIT_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Run Qwen baseline on dev split"))
            .andExpect(jsonPath("$.assigneeId").value(8))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskSuggestionService).update(1L, 21L, 40L, new AcceptTaskSuggestionRequest(
            "Run Qwen baseline on dev split",
            "Record accuracy",
            8L,
            12L,
            LocalDate.of(2026, 10, 10)));
    }

    @Test
    void explicitNullClearsAssigneeWhileAnOmittedAssigneeDoesNot() throws Exception {
        when(taskSuggestionService.update(eq(1L), eq(21L), eq(40L), any())).thenReturn(suggestion(40L, TaskSuggestionStatus.PENDING));

        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": null}"))
            .andExpect(status().isOk());

        ArgumentCaptor<AcceptTaskSuggestionRequest> cleared = ArgumentCaptor.forClass(AcceptTaskSuggestionRequest.class);
        verify(taskSuggestionService).update(eq(1L), eq(21L), eq(40L), cleared.capture());
        assertThat(cleared.getValue().has(ReviewedField.ASSIGNEE_ID)).isTrue();
        assertThat(cleared.getValue().assigneeId()).isNull();
        assertThat(cleared.getValue().has(ReviewedField.TITLE)).isFalse();

        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Keep assignee\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<AcceptTaskSuggestionRequest> omitted = ArgumentCaptor.forClass(AcceptTaskSuggestionRequest.class);
        verify(taskSuggestionService, org.mockito.Mockito.times(2)).update(eq(1L), eq(21L), eq(40L), omitted.capture());
        AcceptTaskSuggestionRequest second = omitted.getAllValues().get(1);
        assertThat(second.has(ReviewedField.TITLE)).isTrue();
        assertThat(second.has(ReviewedField.ASSIGNEE_ID)).isFalse();
    }

    @Test
    void acceptsSuggestionWithEditsAndReturnsCreatedTask() throws Exception {
        when(taskSuggestionService.accept(eq(1L), eq(21L), eq(40L), any())).thenReturn(task(500L));

        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EDIT_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(500))
            .andExpect(jsonPath("$.title").value("Run Qwen baseline"))
            .andExpect(jsonPath("$.assigneeId").value(7))
            .andExpect(jsonPath("$.milestoneId").value(11))
            .andExpect(jsonPath("$.dueDate").value("2026-10-05"))
            .andExpect(jsonPath("$.status").value("TODO"));

        verify(taskSuggestionService).accept(1L, 21L, 40L, new AcceptTaskSuggestionRequest(
            "Run Qwen baseline on dev split",
            "Record accuracy",
            8L,
            12L,
            LocalDate.of(2026, 10, 10)));
    }

    @Test
    void acceptsSuggestionWithoutBody() throws Exception {
        when(taskSuggestionService.accept(eq(1L), eq(21L), eq(40L), isNull())).thenReturn(task(500L));

        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/accept"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(500))
            .andExpect(jsonPath("$.status").value("TODO"));

        verify(taskSuggestionService).accept(1L, 21L, 40L, null);
    }

    @Test
    void rejectsSuggestion() throws Exception {
        when(taskSuggestionService.reject(1L, 21L, 40L)).thenReturn(suggestion(40L, TaskSuggestionStatus.REJECTED));

        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/reject"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(40))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.acceptedTaskId").doesNotExist());
    }

    @Test
    void returnsNotFoundForMissingMeeting() throws Exception {
        when(taskSuggestionService.extract(1L, 999L)).thenThrow(failure(HttpStatus.NOT_FOUND));
        when(taskSuggestionService.listByMeeting(1L, 999L)).thenThrow(failure(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/projects/1/meetings/999/task-suggestions/extract"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/1/meetings/999/task-suggestions"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForMissingSuggestion() throws Exception {
        when(taskSuggestionService.update(eq(1L), eq(21L), eq(999L), any())).thenThrow(failure(HttpStatus.NOT_FOUND));
        when(taskSuggestionService.accept(eq(1L), eq(21L), eq(999L), any())).thenThrow(failure(HttpStatus.NOT_FOUND));
        when(taskSuggestionService.reject(1L, 21L, 999L)).thenThrow(failure(HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EDIT_BODY))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/999/accept"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/999/reject"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForInvalidAssigneeOrMilestone() throws Exception {
        when(taskSuggestionService.accept(eq(1L), eq(21L), eq(40L), any())).thenThrow(failure(HttpStatus.BAD_REQUEST));
        when(taskSuggestionService.update(eq(1L), eq(21L), eq(40L), any())).thenThrow(failure(HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": 9}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"milestoneId\": 31}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForMalformedRequestBodies() throws Exception {
        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dueDate\": \"not-a-date\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": \"seven\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/projects/1/meetings/abc/task-suggestions"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(taskSuggestionService);
    }

    @Test
    void returnsConflictForNonPendingSuggestion() throws Exception {
        when(taskSuggestionService.update(eq(1L), eq(21L), eq(40L), any())).thenThrow(failure(HttpStatus.CONFLICT));
        when(taskSuggestionService.accept(eq(1L), eq(21L), eq(40L), any())).thenThrow(failure(HttpStatus.CONFLICT));
        when(taskSuggestionService.reject(1L, 21L, 40L)).thenThrow(failure(HttpStatus.CONFLICT));

        mockMvc.perform(put("/api/projects/1/meetings/21/task-suggestions/40")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EDIT_BODY))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/accept"))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/projects/1/meetings/21/task-suggestions/40/reject"))
            .andExpect(status().isConflict());
    }

    private static ResponseStatusException failure(HttpStatus status) {
        return new ResponseStatusException(status);
    }

    private static TaskSuggestion suggestion(Long id, TaskSuggestionStatus status) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setId(id);
        suggestion.setMeetingId(21L);
        suggestion.setProjectId(1L);
        suggestion.setTitle("Run Qwen baseline");
        suggestion.setDescription("Evaluate on the dev split");
        suggestion.setAssigneeName("Aneruthan");
        suggestion.setAssigneeId(7L);
        suggestion.setMilestoneId(11L);
        suggestion.setDueDate(LocalDate.of(2026, 10, 5));
        suggestion.setSourceExcerpt("ACTION: Aneruthan | Run Qwen baseline | 2026-10-05");
        suggestion.setConfidence(new BigDecimal("0.90"));
        suggestion.setStatus(status);
        return suggestion;
    }

    private static Task task(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Run Qwen baseline");
        task.setDescription("Evaluate on the dev split");
        task.setAssigneeId(7L);
        task.setMilestoneId(11L);
        task.setDueDate(LocalDate.of(2026, 10, 5));
        task.setStatus(TaskStatus.TODO);
        return task;
    }
}
