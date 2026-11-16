package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.research.ResearchExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearchExtractionController.class)
class ResearchExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResearchExtractionService researchExtractionService;

    @Test
    void returnsResearchExtractionWithoutCreatingTasks() throws Exception {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        decision.setConfidence(0.8);
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        response.setDecisions(List.of(decision));
        when(researchExtractionService.extract(1L, 21L)).thenReturn(response);

        mockMvc.perform(post("/api/projects/1/meetings/21/research-extraction"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decisions[0].title").value("Keep the baseline"))
            .andExpect(jsonPath("$.decisions[0].confidence").value(0.8));

        verify(researchExtractionService).extract(1L, 21L);
    }

    @Test
    void returnsNotFoundWhenMeetingIsMissing() throws Exception {
        when(researchExtractionService.extract(1L, 99L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        mockMvc.perform(post("/api/projects/1/meetings/99/research-extraction"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsServiceUnavailableWhenGeminiIsNotConfigured() throws Exception {
        when(researchExtractionService.extract(1L, 21L))
            .thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini research extraction is not configured"));

        mockMvc.perform(post("/api/projects/1/meetings/21/research-extraction"))
            .andExpect(status().isServiceUnavailable());
    }
}
