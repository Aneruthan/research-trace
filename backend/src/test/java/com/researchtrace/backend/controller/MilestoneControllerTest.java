package com.researchtrace.backend.controller;

import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MilestoneController.class)
class MilestoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MilestoneService milestoneService;

    @MockitoBean
    private ProgressService progressService;

    @Test
    void returnsDerivedMilestoneProgress() throws Exception {
        when(milestoneService.getMilestoneById(11L)).thenReturn(Optional.of(new com.researchtrace.backend.model.Milestone()));
        when(progressService.getMilestoneProgress(11L)).thenReturn(new BigDecimal("50.00"));

        mockMvc.perform(get("/api/milestones/11/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("50.00"));

        verify(progressService).getMilestoneProgress(11L);
    }

    @Test
    void returnsNotFoundWhenMilestoneIsMissing() throws Exception {
        when(milestoneService.getMilestoneById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/milestones/99/progress"))
            .andExpect(status().isNotFound());
    }
}
