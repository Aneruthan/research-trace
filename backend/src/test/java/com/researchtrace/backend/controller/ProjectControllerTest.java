package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.service.ProgressService;
import com.researchtrace.backend.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProgressService progressService;

    @Test
    void returnsProjectProgress() throws Exception {
        when(progressService.getProjectProgress(1L)).thenReturn(new BigDecimal("70.00"));

        mockMvc.perform(get("/api/projects/1/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("70.00"));
    }

    @Test
    void returnsZeroProgressForProjectWithoutMilestones() throws Exception {
        when(progressService.getProjectProgress(any())).thenReturn(new BigDecimal("0.00"));

        mockMvc.perform(get("/api/projects/9/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("0.00"));
    }

    @Test
    void stillServesTheExistingProjectEndpoint() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setName("ResearchTrace");
        project.setStatus(ProjectStatus.ACTIVE);

        when(projectService.getProjectById(1L)).thenReturn(Optional.of(project));

        mockMvc.perform(get("/api/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("ResearchTrace"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
