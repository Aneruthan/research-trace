package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.research.DecisionLineageView;
import com.researchtrace.backend.dto.research.ExperimentRequest;
import com.researchtrace.backend.dto.research.ResearchDecisionRequest;
import com.researchtrace.backend.dto.research.ResearchTraceSummary;
import com.researchtrace.backend.dto.research.ResultRequest;
import com.researchtrace.backend.dto.research.SupersedeDecisionRequest;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.research.ChangeDetectionService;
import com.researchtrace.backend.research.ResearchTraceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/research")
public class ResearchTraceController {

    private final ResearchTraceService researchTraceService;
    private final ChangeDetectionService changeDetectionService;

    public ResearchTraceController(
            ResearchTraceService researchTraceService,
            ChangeDetectionService changeDetectionService) {
        this.researchTraceService = researchTraceService;
        this.changeDetectionService = changeDetectionService;
    }

    // --- Decisions ---

    @GetMapping("/decisions")
    public List<ResearchDecision> getDecisions(
            @PathVariable Long projectId,
            @RequestParam(required = false) ResearchDecisionStatus status) {
        return researchTraceService.getDecisions(projectId, status);
    }

    @PostMapping("/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResearchDecision createDecision(
            @PathVariable Long projectId,
            @Valid @RequestBody ResearchDecisionRequest request) {
        return researchTraceService.createDecision(projectId, request);
    }

    @GetMapping("/decisions/{decisionId}")
    public ResearchDecision getDecision(
            @PathVariable Long projectId,
            @PathVariable Long decisionId) {
        return researchTraceService.getDecision(projectId, decisionId);
    }

    @PostMapping("/decisions/{decisionId}/supersede")
    @ResponseStatus(HttpStatus.CREATED)
    public ResearchDecision supersedeDecision(
            @PathVariable Long projectId,
            @PathVariable Long decisionId,
            @Valid @RequestBody SupersedeDecisionRequest request) {
        return researchTraceService.supersedeDecision(projectId, decisionId, request);
    }

    @GetMapping("/decisions/{decisionId}/lineage")
    public DecisionLineageView getDecisionLineage(
            @PathVariable Long projectId,
            @PathVariable Long decisionId) {
        return researchTraceService.getDecisionLineage(projectId, decisionId);
    }

    @GetMapping("/decisions/{decisionId}/tasks")
    public List<Task> getTasksForDecision(
            @PathVariable Long projectId,
            @PathVariable Long decisionId) {
        return researchTraceService.getTasksForDecision(projectId, decisionId);
    }

    // --- Experiments ---

    @GetMapping("/experiments")
    public List<Experiment> getExperiments(@PathVariable Long projectId) {
        return researchTraceService.getExperiments(projectId);
    }

    @PostMapping("/experiments")
    @ResponseStatus(HttpStatus.CREATED)
    public Experiment createExperiment(
            @PathVariable Long projectId,
            @Valid @RequestBody ExperimentRequest request) {
        return researchTraceService.createExperiment(projectId, request);
    }

    @GetMapping("/experiments/{experimentId}")
    public Experiment getExperiment(
            @PathVariable Long projectId,
            @PathVariable Long experimentId) {
        return researchTraceService.getExperiment(projectId, experimentId);
    }

    @GetMapping("/experiments/{experimentId}/results")
    public List<Result> getResultsForExperiment(
            @PathVariable Long projectId,
            @PathVariable Long experimentId) {
        return researchTraceService.getResultsForExperiment(projectId, experimentId);
    }

    // --- Results ---

    @GetMapping("/results")
    public List<Result> getResults(@PathVariable Long projectId) {
        return researchTraceService.getResults(projectId);
    }

    @PostMapping("/results")
    @ResponseStatus(HttpStatus.CREATED)
    public Result recordResult(
            @PathVariable Long projectId,
            @Valid @RequestBody ResultRequest request) {
        return researchTraceService.recordResult(projectId, request);
    }

    // --- Timeline & Changes ---

    @GetMapping("/timeline")
    public List<ResearchEvent> getTimeline(@PathVariable Long projectId) {
        return researchTraceService.getTimeline(projectId);
    }

    @GetMapping("/changes")
    public List<ResearchChange> getChanges(
            @PathVariable Long projectId,
            @RequestParam(required = false) ResearchChangeStatus status) {
        return researchTraceService.getChanges(projectId, status);
    }

    @PutMapping("/changes/{changeId}/status")
    public ResearchChange updateChangeStatus(
            @PathVariable Long projectId,
            @PathVariable Long changeId,
            @RequestParam ResearchChangeStatus status) {
        return changeDetectionService.updateChangeStatus(projectId, changeId, status);
    }

    // --- Summary ---

    @GetMapping("/summary")
    public ResearchTraceSummary getSummary(@PathVariable Long projectId) {
        return researchTraceService.getSummary(projectId);
    }
}
