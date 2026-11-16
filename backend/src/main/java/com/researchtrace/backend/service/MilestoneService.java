package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.TextLimits;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;

    public MilestoneService(MilestoneRepository milestoneRepository, TaskRepository taskRepository) {
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
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
        if (milestone != null) {
            rejectLongTitle(milestone.getTitle());
        }
        return milestoneRepository.save(milestone);
    }

    public Milestone updateMilestone(Long id, Milestone patch) {
        return applyUpdate(id, patch, patch != null && patch.getWeight() != null);
    }

    public Milestone updateMilestone(Long id, MilestoneUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone details are required");
        }
        Milestone patch = new Milestone();
        patch.setProjectId(request.getProjectId());
        patch.setTitle(request.getTitle());
        patch.setDescription(request.getDescription());
        patch.setStatus(request.getStatus());
        patch.setDueDate(request.getDueDate());
        patch.setWeight(request.getWeight());
        return applyUpdate(id, patch, request.hasWeight());
    }

    private Milestone applyUpdate(Long id, Milestone patch, boolean weightPresent) {
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
            rejectLongTitle(title);
            existing.setTitle(title);
        }
        if (patch.getDescription() != null) {
            String description = patch.getDescription().trim();
            existing.setDescription(description.isEmpty() ? null : description);
        }
        if (patch.getStatus() != null) {
            existing.setStatus(patch.getStatus());
        }
        if (weightPresent) {
            if (patch.getWeight() != null && patch.getWeight().signum() < 0) {
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

    private static void rejectLongTitle(String title) {
        if (title != null && title.length() > TextLimits.TITLE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 255 characters");
        }
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
        rejectLongTitle(title);
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
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone ID is required");
        }
        if (milestoneRepository.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found: " + id);
        }
        if (taskRepository != null && !taskRepository.findByMilestoneId(id).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot delete this milestone because tasks are still associated with it. Reassign or remove those tasks first.");
        }
        milestoneRepository.deleteById(id);
    }
}