package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/projects/{projectId}/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final ProgressService progressService;

    public MilestoneController(MilestoneService milestoneService, ProgressService progressService) {
        this.milestoneService = milestoneService;
        this.progressService = progressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Milestone createMilestone(
            @PathVariable Long projectId,
            @RequestBody Milestone milestone) {
        return milestoneService.createForProject(projectId, milestone);
    }

    @GetMapping("/{milestoneId}")
    public Milestone getMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {
        return milestoneService.getMilestoneForProject(projectId, milestoneId);
    }

    @GetMapping("/{milestoneId}/progress")
    public BigDecimal getMilestoneProgress(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {
        milestoneService.getMilestoneForProject(projectId, milestoneId);
        return progressService.getMilestoneProgress(milestoneId, projectId);
    }

    @PutMapping("/{milestoneId}")
    public Milestone updateMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest milestone) {
        return milestoneService.updateForProject(projectId, milestoneId, milestone);
    }

    @DeleteMapping("/{milestoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {
        milestoneService.deleteForProject(projectId, milestoneId);
    }
}
