package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.repository.MilestoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(MilestoneRepository milestoneRepository) {
        this.milestoneRepository = milestoneRepository;
    }

    public List<Milestone> getAllMilestones() {
        return milestoneRepository.findAll();
    }

    public Optional<Milestone> getMilestoneById(Long id) {
        return milestoneRepository.findById(id);
    }

    public List<Milestone> getMilestonesByProject(Long projectId) {
        return milestoneRepository.findByProjectId(projectId);
    }

    public Milestone saveMilestone(Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    public Milestone updateMilestoneFromExtraction(
        Long milestoneId,
        String title,
        String description,
        java.time.LocalDate dueDate) {

    Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Milestone not found: " + milestoneId));

    if (title != null) {
        milestone.setTitle(title);
    }
    if (description != null) {
        milestone.setDescription(description);
    }
    if (dueDate != null) {
        milestone.setDueDate(dueDate);
    }
    return milestoneRepository.save(milestone);
    }

    public void deleteMilestone(Long id) {
        milestoneRepository.deleteById(id);
    }
}