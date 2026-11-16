package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    private final ProgressService progressService;

    public MilestoneController(MilestoneService milestoneService, ProgressService progressService) {
        this.milestoneService = milestoneService;
        this.progressService = progressService;
    }

    @GetMapping
    public List<Milestone> getAllMilestones() {
        return milestoneService.getAllMilestones();
    }

    @PostMapping
    public Milestone createMilestone(@RequestBody Milestone milestone) {
        return milestoneService.saveMilestone(milestone);
    }

    @GetMapping("/{id}")
    public Milestone getMilestoneById(@PathVariable Long id) {
        return milestoneService.getMilestoneById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found: " + id));
    }

    @GetMapping("/{milestoneId}/progress")
    public BigDecimal getMilestoneProgress(
            @PathVariable Long milestoneId,
            @RequestParam(required = false) Long projectId) {
        Milestone milestone = milestoneService.getMilestoneById(milestoneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found: " + milestoneId));
        Long effectiveProjectId = projectId != null ? projectId : milestone.getProjectId();
        return effectiveProjectId != null
            ? progressService.getMilestoneProgress(milestoneId, effectiveProjectId)
            : progressService.getMilestoneProgress(milestoneId);
    }

    @PutMapping("/{id}")
    public Milestone updateMilestone(
        @PathVariable Long id,
        @Valid @RequestBody MilestoneUpdateRequest milestone) {

        return milestoneService.updateMilestone(id, milestone);
    }

    @DeleteMapping("/{id}")
    public void deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
    }
}
