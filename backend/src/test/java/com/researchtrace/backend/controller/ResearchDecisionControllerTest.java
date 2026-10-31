package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.DecisionSupersession;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.service.ResearchDecisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearchDecisionController.class)
class ResearchDecisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResearchDecisionService researchDecisionService;

    @Test
    void supersedesAnExistingDecision() throws Exception {
        ResearchDecision previous = decision(3L, "Keep the baseline", ResearchDecisionStatus.SUPERSEDED, null);
        ResearchDecision replacement = decision(4L, "Use a balanced split", ResearchDecisionStatus.ACTIVE, 3L);
        when(researchDecisionService.supersede(1L, 3L, 4L))
            .thenReturn(new DecisionSupersession(previous, replacement));

        mockMvc.perform(post("/api/projects/1/research-decisions/3/supersede")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"replacementDecisionId\":4}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.superseded.id").value(3))
            .andExpect(jsonPath("$.superseded.status").value("SUPERSEDED"))
            .andExpect(jsonPath("$.replacement.id").value(4))
            .andExpect(jsonPath("$.replacement.status").value("ACTIVE"))
            .andExpect(jsonPath("$.replacement.supersedesDecisionId").value(3));
    }

    @Test
    void missingReplacementIsBadRequest() throws Exception {
        when(researchDecisionService.supersede(1L, 3L, null))
            .thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Decision id and replacement decision id are required"));

        mockMvc.perform(post("/api/projects/1/research-decisions/3/supersede")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    private static ResearchDecision decision(Long id, String title, ResearchDecisionStatus status, Long supersedesDecisionId) {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(id);
        decision.setProjectId(1L);
        decision.setTitle(title);
        decision.setStatus(status);
        decision.setSupersedesDecisionId(supersedesDecisionId);
        return decision;
    }
}
