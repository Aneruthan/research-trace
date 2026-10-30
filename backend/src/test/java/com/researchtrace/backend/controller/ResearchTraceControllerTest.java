package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.research.DecisionLineageView;
import com.researchtrace.backend.dto.research.DecisionLineageView.ExperimentEvidence;
import com.researchtrace.backend.dto.research.ResearchTraceSummary;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.research.ResearchTraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearchTraceController.class)
class ResearchTraceControllerTest {

    private static final Long PROJECT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResearchTraceService researchTraceService;

    @Test
    void listsDecisionsForProject() throws Exception {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        decision.setTitle("Evaluate PMC-VQA");
        decision.setStatus(ResearchDecisionStatus.ACTIVE);

        when(researchTraceService.getDecisions(PROJECT_ID, null)).thenReturn(List.of(decision));

        mockMvc.perform(get("/api/projects/1/research/decisions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].title").value("Evaluate PMC-VQA"));
    }

    @Test
    void createsDecisionSuccessfully() throws Exception {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(10L);
        decision.setProjectId(PROJECT_ID);
        decision.setTitle("Evaluate PMC-VQA");
        decision.setStatus(ResearchDecisionStatus.ACTIVE);

        when(researchTraceService.createDecision(eq(PROJECT_ID), any())).thenReturn(decision);

        mockMvc.perform(post("/api/projects/1/research/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Evaluate PMC-VQA",
                      "decisionText": "Use standard evaluation benchmark",
                      "rationale": "High relevance to clinical questions",
                      "decidedAt": "2026-10-10"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("Evaluate PMC-VQA"));
    }

    @Test
    void rejectsDecisionWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/projects/1/research/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "  ",
                      "rationale": "Some rationale"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void supersedesDecision() throws Exception {
        ResearchDecision newDecision = new ResearchDecision();
        newDecision.setId(11L);
        newDecision.setProjectId(PROJECT_ID);
        newDecision.setTitle("Switch to LitMedImage");
        newDecision.setSupersedesDecisionId(10L);
        newDecision.setStatus(ResearchDecisionStatus.ACTIVE);

        when(researchTraceService.supersedeDecision(eq(PROJECT_ID), eq(10L), any())).thenReturn(newDecision);

        mockMvc.perform(post("/api/projects/1/research/decisions/10/supersede")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Switch to LitMedImage",
                      "rationale": "Label noise in PMC-VQA prevented training convergence."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(11))
            .andExpect(jsonPath("$.supersedesDecisionId").value(10));
    }

    @Test
    void retrievesDecisionLineage() throws Exception {
        ResearchDecision current = new ResearchDecision();
        current.setId(11L);
        current.setTitle("LitMedImage");

        ResearchDecision prev = new ResearchDecision();
        prev.setId(10L);
        prev.setTitle("PMC-VQA");

        Experiment exp = new Experiment();
        exp.setId(50L);
        exp.setTitle("Benchmark run");

        Result res = new Result();
        res.setId(60L);
        res.setSummary("Accuracy 75%");

        DecisionLineageView view = new DecisionLineageView();
        view.setDecision(current);
        view.setPredecessorDecision(prev);
        view.setExperiments(List.of(new ExperimentEvidence(exp, List.of(res))));

        when(researchTraceService.getDecisionLineage(PROJECT_ID, 11L)).thenReturn(view);

        mockMvc.perform(get("/api/projects/1/research/decisions/11/lineage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision.id").value(11))
            .andExpect(jsonPath("$.predecessorDecision.id").value(10))
            .andExpect(jsonPath("$.experiments[0].experiment.id").value(50))
            .andExpect(jsonPath("$.experiments[0].results[0].id").value(60));
    }

    @Test
    void createsExperimentAndRecordsResult() throws Exception {
        Experiment exp = new Experiment();
        exp.setId(50L);
        exp.setProjectId(PROJECT_ID);
        exp.setTitle("Zero-shot CLIP test");
        exp.setStatus(ExperimentStatus.PLANNED);

        when(researchTraceService.createExperiment(eq(PROJECT_ID), any())).thenReturn(exp);

        mockMvc.perform(post("/api/projects/1/research/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Zero-shot CLIP test",
                      "hypothesis": "Achieves >60% accuracy",
                      "status": "PLANNED"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(50));

        Result result = new Result();
        result.setId(60L);
        result.setProjectId(PROJECT_ID);
        result.setExperimentId(50L);
        result.setSummary("Result observation");

        when(researchTraceService.recordResult(eq(PROJECT_ID), any())).thenReturn(result);

        mockMvc.perform(post("/api/projects/1/research/results")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "experimentId": 50,
                      "summary": "Result observation"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(60));
    }

    @Test
    void getsTimelineAndSummary() throws Exception {
        ResearchEvent event = new ResearchEvent();
        event.setId(100L);
        event.setTitle("Decision 1");
        event.setEventType(ResearchEventType.DECISION);
        event.setOccurredAt(LocalDateTime.of(2026, 10, 10, 10, 0));

        when(researchTraceService.getTimeline(PROJECT_ID)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/projects/1/research/timeline"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].eventType").value("DECISION"));

        ResearchTraceSummary summary = new ResearchTraceSummary();
        summary.setTotalDecisions(5);
        summary.setActiveDecisions(3);

        when(researchTraceService.getSummary(PROJECT_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/projects/1/research/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalDecisions").value(5))
            .andExpect(jsonPath("$.activeDecisions").value(3));
    }

    @Test
    void returnsNotFoundWhenProjectDoesNotExist() throws Exception {
        when(researchTraceService.getDecisions(999L, null))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: 999"));

        mockMvc.perform(get("/api/projects/999/research/decisions"))
            .andExpect(status().isNotFound());
    }
}
