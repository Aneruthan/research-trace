package com.researchtrace.backend.config;

import com.researchtrace.backend.ai.TaskExtractionException;
import com.researchtrace.backend.controller.TaskSuggestionController;
import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.controller.MilestoneController;
import com.researchtrace.backend.controller.ResearchTraceController;
import com.researchtrace.backend.service.TaskSuggestionService;
import com.researchtrace.backend.research.ChangeDetectionService;
import com.researchtrace.backend.research.ResearchTraceService;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MilestoneController.class, ResearchTraceController.class, TaskSuggestionController.class})
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MilestoneService milestoneService;

    @MockitoBean
    private ProgressService progressService;

    @MockitoBean
    private ResearchTraceService researchTraceService;

    @MockitoBean
    private ChangeDetectionService changeDetectionService;

    @MockitoBean
    private TaskSuggestionService taskSuggestionService;

    @Test
    void serviceRejectionReturnsItsReason() throws Exception {
        when(milestoneService.updateForProject(eq(1L), eq(5L), any(MilestoneUpdateRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone weight cannot be negative"));

        mockMvc.perform(put("/api/projects/1/milestones/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pilot\",\"weight\":-1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Milestone weight cannot be negative"))
            .andExpect(jsonPath("$.path").value("/api/projects/1/milestones/5"));
    }

    @Test
    void conflictKeepsItsStatusAndReason() throws Exception {
        when(researchTraceService.supersedeDecision(eq(1L), eq(3L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Decision 3 is already superseded."));

        mockMvc.perform(post("/api/projects/1/research/decisions/3/supersede")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New direction\",\"rationale\":\"Better data\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Decision 3 is already superseded."));
    }

    @Test
    void overlongTitleIsRejectedWithValidationMessage() throws Exception {
        String title = "t".repeat(256);

        mockMvc.perform(post("/api/projects/1/research/decisions/3/supersede")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"rationale\":\"Better data\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Title must be at most 255 characters"));

        verify(researchTraceService, never()).supersedeDecision(any(), any(), any());
    }

    @Test
    void taskExtractionFailureReturnsASafeMessage() throws Exception {
        when(taskSuggestionService.extract(1L, 2L)).thenThrow(new TaskExtractionException(
            "Task extraction failed. The model response could not be used."));

        mockMvc.perform(post("/api/projects/1/meetings/2/task-suggestions/extract"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value(
                "Task extraction failed. The model response could not be used."));
    }
}
