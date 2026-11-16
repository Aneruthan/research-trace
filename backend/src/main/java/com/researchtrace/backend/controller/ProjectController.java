package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.dto.ProjectWriteRequest;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.dto.DeletionImpact;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.service.MeetingService;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import com.researchtrace.backend.service.ProjectDashboardService;
import com.researchtrace.backend.service.ProjectMemberService;
import com.researchtrace.backend.service.ProjectService;
import com.researchtrace.backend.service.ResearchQueryService;
import com.researchtrace.backend.service.TaskService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

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

    @GetMapping("/{projectId}/meetings/{meetingId}")
    public Meeting getMeetingForProject(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return meetingService.getMeetingForProject(projectId, meetingId);
    }

    @PostMapping("/{projectId}/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public Meeting createMeetingForProject(
            @PathVariable Long projectId,
            @RequestBody Meeting meeting) {
        return meetingService.createMeeting(projectId, meeting);
    }

    @PutMapping("/{projectId}/meetings/{meetingId}")
    public Meeting updateMeetingForProject(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @RequestBody Meeting meeting) {
        return meetingService.updateMeeting(projectId, meetingId, meeting);
    }

    @GetMapping("/{projectId}/meetings/{meetingId}/deletion-impact")
    public DeletionImpact meetingDeletionImpact(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return meetingService.deletionImpact(projectId, meetingId);
    }

    @DeleteMapping("/{projectId}/meetings/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeetingForProject(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        meetingService.deleteMeeting(meetingId, projectId);
    }

    @GetMapping("/{projectId}/tasks")
    public List<Task> getTasksByProject(@PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId);
    }

    @PostMapping("/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTaskForProject(
            @PathVariable Long projectId,
            @RequestBody Task task) {
        if (projectService.getProjectById(projectId).isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Project not found");
        }
        return taskService.createForProject(projectId, task);
    }

    @GetMapping("/{projectId}/tasks/{taskId}")
    public Task getTaskForProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return taskService.getTaskForProject(projectId, taskId);
    }

    @GetMapping("/{projectId}/tasks/{taskId}/history")
    public List<TaskHistory> getTaskHistoryForProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return taskService.getTaskHistoryForProject(projectId, taskId);
    }

    @PutMapping("/{projectId}/tasks/{taskId}")
    public Task updateTaskForProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody Task task,
            @RequestParam(required = false) Long userId) {
        return taskService.updateTaskForProject(projectId, taskId, task, userId);
    }

    @DeleteMapping("/{projectId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskForProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        taskService.deleteTaskForProject(projectId, taskId);
    }

    @GetMapping("/{projectId}/milestones/{milestoneId}/tasks")
    public List<Task> getTasksByMilestoneForProject(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId) {
        return taskService.getTasksByMilestoneForProject(projectId, milestoneId);
    }

    @GetMapping("/{projectId}/assignees/{assigneeId}/tasks")
    public List<Task> getTasksByAssigneeForProject(
            @PathVariable Long projectId,
            @PathVariable Long assigneeId) {
        return taskService.getTasksByAssigneeForProject(projectId, assigneeId);
    }

    @GetMapping("/{projectId}/research-decisions/{decisionId}/tasks")
    public List<Task> getTasksByResearchDecisionForProject(
            @PathVariable Long projectId,
            @PathVariable Long decisionId) {
        return taskService.getTasksByResearchDecisionForProject(projectId, decisionId);
    }

    @GetMapping("/{projectId}/experiments/{experimentId}/tasks")
    public List<Task> getTasksByExperimentForProject(
            @PathVariable Long projectId,
            @PathVariable Long experimentId) {
        return taskService.getTasksByExperimentForProject(projectId, experimentId);
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
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@RequestBody ProjectWriteRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody ProjectWriteRequest request) {
        return projectService.updateProject(id, request);
    }

    @PostMapping("/{id}/archive")
    public Project archiveProject(@PathVariable Long id) {
        return projectService.archiveProject(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}