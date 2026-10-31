package com.researchtrace.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private TaskService taskService;

    @Test
    void getTaskByIdReturnsTask() throws Exception {
        Task task = new Task();
        task.setId(10L);
        task.setTitle("Test task");
        task.setStatus(TaskStatus.TODO);

        when(taskService.getTaskById(10L)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/tasks/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("Test task"));
    }

    @Test
    void getTaskByIdReturnsNotFoundWhenMissing() throws Exception {
        when(taskService.getTaskById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tasks/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTaskHistoryReturnsChronologicalList() throws Exception {
        TaskHistory h1 = new TaskHistory(10L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, null, LocalDateTime.of(2026, 10, 1, 10, 0));
        h1.setId(1L);
        TaskHistory h2 = new TaskHistory(10L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, 5L, LocalDateTime.of(2026, 10, 2, 11, 0));
        h2.setId(2L);

        when(taskService.getTaskHistory(10L)).thenReturn(List.of(h1, h2));

        mockMvc.perform(get("/api/tasks/10/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].taskId").value(10))
            .andExpect(jsonPath("$[0].previousStatus").value("TODO"))
            .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$[0].changedByUserId").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].previousStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$[1].newStatus").value("COMPLETED"))
            .andExpect(jsonPath("$[1].changedByUserId").value(5));

        verify(taskService).getTaskHistory(10L);
    }

    @Test
    void getTaskHistoryReturnsNotFoundWhenTaskMissing() throws Exception {
        when(taskService.getTaskHistory(99L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: 99"));

        mockMvc.perform(get("/api/tasks/99/history"))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateTaskPassesUserIdToService() throws Exception {
        Task update = new Task();
        update.setTitle("Updated task");
        update.setStatus(TaskStatus.COMPLETED);

        Task returned = new Task();
        returned.setId(10L);
        returned.setTitle("Updated task");
        returned.setStatus(TaskStatus.COMPLETED);

        when(taskService.saveTask(any(Task.class), eq(42L))).thenReturn(returned);

        mockMvc.perform(put("/api/tasks/10?userId=42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(taskService).saveTask(any(Task.class), eq(42L));
    }

    @Test
    void getTasksByResearchDecisionReturnsTasks() throws Exception {
        Task task = new Task();
        task.setId(15L);
        task.setTitle("Decision task");
        task.setResearchDecisionId(55L);

        when(taskService.getTasksByResearchDecision(55L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks/research-decision/55"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(15))
            .andExpect(jsonPath("$[0].researchDecisionId").value(55));
    }

    @Test
    void getTasksByExperimentReturnsTasks() throws Exception {
        Task task = new Task();
        task.setId(16L);
        task.setTitle("Experiment task");
        task.setExperimentId(66L);

        when(taskService.getTasksByExperiment(66L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks/experiment/66"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(16))
            .andExpect(jsonPath("$[0].experimentId").value(66));
    }
}