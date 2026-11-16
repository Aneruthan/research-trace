package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Test
    void rejectsAMilestoneTitleLongerThan255() throws Exception {
        mockMvc.perform(put("/api/milestones/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "m".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Title must be at most 255 characters"));

        verify(milestoneService, never()).updateMilestone(eq(5L), any(com.researchtrace.backend.dto.MilestoneUpdateRequest.class));
    }

    @Test
    void deleteConflictAsksForTasksToBeMovedFirst() throws Exception {
        doThrow(new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Cannot delete this milestone because tasks are still associated with it. Reassign or remove those tasks first."))
            .when(milestoneService).deleteMilestone(5L);

        mockMvc.perform(delete("/api/milestones/5"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "Cannot delete this milestone because tasks are still associated with it. Reassign or remove those tasks first."));
    }

    @Test
    void explicitNullWeightIsDistinctFromAnOmittedWeight() throws Exception {
        when(milestoneService.updateMilestone(eq(5L), any(MilestoneUpdateRequest.class)))
            .thenReturn(new Milestone());
        ArgumentCaptor<MilestoneUpdateRequest> updates = ArgumentCaptor.forClass(MilestoneUpdateRequest.class);

        mockMvc.perform(put("/api/milestones/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weight\":null}"))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/milestones/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Baseline\"}"))
            .andExpect(status().isOk());

        verify(milestoneService, org.mockito.Mockito.times(2)).updateMilestone(eq(5L), updates.capture());
        assertThat(updates.getAllValues().get(0).hasWeight()).isTrue();
        assertThat(updates.getAllValues().get(0).getWeight()).isNull();
        assertThat(updates.getAllValues().get(1).hasWeight()).isFalse();
    }
}
