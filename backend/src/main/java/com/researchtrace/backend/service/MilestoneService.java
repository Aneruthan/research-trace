package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.repository.MilestoneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Milestone updateMilestone(Long id, Milestone patch) {
        Milestone existing = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found: " + id));
        if (patch == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone details are required");
        }
        if (patch.getTitle() != null) {
            String title = patch.getTitle().trim();
            if (title.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone title cannot be empty");
            }
            existing.setTitle(title);
        }
        if (patch.getDescription() != null) {
            String description = patch.getDescription().trim();
            existing.setDescription(description.isEmpty() ? null : description);
        }
        if (patch.getStatus() != null) {
            existing.setStatus(patch.getStatus());
        }
        if (patch.getWeight() != null) {
            if (patch.getWeight().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone weight cannot be negative");
            }
            existing.setWeight(patch.getWeight());
        }
        existing.setDueDate(patch.getDueDate());
        if (patch.getProjectId() != null
                && existing.getProjectId() != null
                && !patch.getProjectId().equals(existing.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone cannot be moved to another project");
        }
        return milestoneRepository.save(existing);
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