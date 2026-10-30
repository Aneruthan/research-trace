package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.service.MeetingService;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import com.researchtrace.backend.service.ProjectDashboardService;
import com.researchtrace.backend.service.ProjectMemberService;
import com.researchtrace.backend.service.ProjectService;
import com.researchtrace.backend.service.ResearchQueryService;
import com.researchtrace.backend.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    private final ProgressService progressService;

    private final MilestoneService milestoneService;

    private final ProjectMemberService projectMemberService;

    private final MeetingService meetingService;

    private final TaskService taskService;

    private final ProjectDashboardService projectDashboardService;

    private final ResearchQueryService researchQueryService;

    public ProjectController(
            ProjectService projectService,
            ProgressService progressService,
            MilestoneService milestoneService,
            ProjectMemberService projectMemberService,
            MeetingService meetingService,
            TaskService taskService,
            ProjectDashboardService projectDashboardService,
            ResearchQueryService researchQueryService) {

        this.projectService = projectService;
        this.progressService = progressService;
        this.milestoneService = milestoneService;
        this.projectMemberService = projectMemberService;
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.projectDashboardService = projectDashboardService;
        this.researchQueryService = researchQueryService;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id)
            .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @GetMapping("/{projectId}/progress")
    public BigDecimal getProjectProgress(@PathVariable Long projectId) {
        return progressService.getProjectProgress(projectId);
    }

    @GetMapping("/{projectId}/milestones")
    public List<Milestone> getMilestonesByProject(@PathVariable Long projectId) {
        return milestoneService.getMilestonesByProject(projectId);
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMember> getMembersByProject(@PathVariable Long projectId) {
        return projectMemberService.getMembersByProject(projectId);
    }

    @GetMapping("/{projectId}/meetings")
    public List<Meeting> getMeetingsByProject(@PathVariable Long projectId) {
        return meetingService.getMeetingsByProject(projectId);
    }

    @GetMapping("/{projectId}/tasks")
    public List<Task> getTasksByProject(@PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId);
    }

    @GetMapping("/{projectId}/dashboard")
    public ProjectDashboard getDashboard(@PathVariable Long projectId) {
        return projectDashboardService.getDashboard(projectId);
    }

    @GetMapping("/{projectId}/research-decisions")
    public List<ResearchDecision> getResearchDecisions(@PathVariable Long projectId) {
        return researchQueryService.getResearchDecisions(projectId);
    }

    @GetMapping("/{projectId}/experiments")
    public List<Experiment> getExperiments(@PathVariable Long projectId) {
        return researchQueryService.getExperiments(projectId);
    }

    @GetMapping("/{projectId}/results")
    public List<Result> getResults(@PathVariable Long projectId) {
        return researchQueryService.getResults(projectId);
    }

    @GetMapping("/{projectId}/research-events")
    public List<ResearchEvent> getResearchEvents(@PathVariable Long projectId) {
        return researchQueryService.getResearchEvents(projectId);
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.saveProject(project);
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        return projectService.saveProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}