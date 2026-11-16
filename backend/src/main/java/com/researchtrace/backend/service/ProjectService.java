package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectWriteRequest;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskSuggestionRepository taskSuggestionRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ResearchChangeRepository researchChangeRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            MeetingRepository meetingRepository,
            MilestoneRepository milestoneRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository,
            TaskSuggestionRepository taskSuggestionRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository,
            ResearchChangeRepository researchChangeRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
        this.milestoneRepository = milestoneRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
        this.researchChangeRepository = researchChangeRepository;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public Project createProject(ProjectWriteRequest request) {
        Project project = new Project();
        applyProfile(project, request);
        project.setStatus(ProjectStatus.ACTIVE);
        return projectRepository.save(project);
    }

    public Project updateProject(Long id, ProjectWriteRequest request) {
        Project project = requireProject(id);
        applyProfile(project, request);
        return projectRepository.save(project);
    }

    public Project archiveProject(Long id) {
        Project project = requireProject(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        return projectRepository.save(project);
    }

    /**
     * Deletes a project only when it has no dependent records. Archive is the
     * operation that retires a project while keeping its history.
     */
    public void deleteProject(Long id) {
        requireProject(id);
        List<String> dependents = new ArrayList<>();
        if (meetingRepository.existsByProjectId(id)) {
            dependents.add("meetings");
        }
        if (milestoneRepository.existsByProjectId(id)) {
            dependents.add("milestones");
        }
        if (projectMemberRepository.existsByProjectId(id)) {
            dependents.add("memberships");
        }
        if (taskRepository.existsByProjectId(id)) {
            dependents.add("tasks");
        }
        if (taskSuggestionRepository.existsByProjectId(id)) {
            dependents.add("task suggestions");
        }
        if (researchDecisionRepository.existsByProjectId(id)) {
            dependents.add("research decisions");
        }
        if (experimentRepository.existsByProjectId(id)) {
            dependents.add("experiments");
        }
        if (resultRepository.existsByProjectId(id)) {
            dependents.add("results");
        }
        if (researchEventRepository.existsByProjectId(id)) {
            dependents.add("research events");
        }
        if (researchChangeRepository.existsByProjectId(id)) {
            dependents.add("research changes");
        }
        if (!dependents.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot delete this project because it still has " + String.join(", ", dependents)
                    + ". Archive the project to keep those records.");
        }
        projectRepository.deleteById(id);
    }

    private Project requireProject(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ID is required");
        }
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private void applyProfile(Project project, ProjectWriteRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project details are required");
        }
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        if (name.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name must be at most 255 characters");
        }
        LocalDate startDate = request.getStartDate();
        LocalDate targetDate = request.getTargetDate();
        if (startDate != null && targetDate != null && startDate.isAfter(targetDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Start date must not be later than the target completion date");
        }
        Long supervisorId = request.getSupervisorId();
        if (supervisorId != null && !userRepository.existsById(supervisorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supervisor not found");
        }
        project.setName(name);
        project.setDescription(optionalText(request.getDescription(), 255, "Description"));
        project.setObjectives(optionalText(request.getObjectives(), 4000, "Research objectives"));
        project.setStartDate(startDate);
        project.setTargetDate(targetDate);
        project.setSupervisorId(supervisorId);
    }

    private static String optionalText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                label + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
