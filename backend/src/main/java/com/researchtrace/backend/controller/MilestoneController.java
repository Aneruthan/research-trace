package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.service.MilestoneService;
import org.springframework.web.bind.annotation.*;
import com.researchtrace.backend.dto.MilestoneUpdateFromExtractionRequest;
import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
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
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
    }

    @PutMapping("/{id}")
    public Milestone updateMilestone(
        @PathVariable Long id,
        @RequestBody Milestone milestone) {

        milestone.setId(id);
        return milestoneService.saveMilestone(milestone);
    }

    @PostMapping("/update-from-extraction")
    public Milestone updateMilestoneFromExtraction(
        @RequestBody MilestoneUpdateFromExtractionRequest request) {

    return milestoneService.updateMilestoneFromExtraction(
            request.getMilestoneId(),
            request.getTitle(),
            request.getDescription(),
            request.getDueDate());
    }
    
    @DeleteMapping("/{id}")
    public void deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
    }
}