package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.dto.ProjectDashboard.MilestoneProgressItem;
import com.researchtrace.backend.dto.ProjectDashboard.ResearchMetrics;
import com.researchtrace.backend.dto.ProjectDashboard.StudentProgressItem;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectDashboardService {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final MilestoneService milestoneService;
    private final MeetingService meetingService;
    private final TaskService taskService;
    private final TaskSuggestionService taskSuggestionService;
    private final ProgressService progressService;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ResearchChangeRepository researchChangeRepository;

    @Autowired
    public ProjectDashboardService(
            ProjectService projectService,
            ProjectMemberService projectMemberService,
            MilestoneService milestoneService,
            MeetingService meetingService,
            TaskService taskService,
            TaskSuggestionService taskSuggestionService,
            ProgressService progressService,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository,
            ResearchChangeRepository researchChangeRepository) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.milestoneService = milestoneService;
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.taskSuggestionService = taskSuggestionService;
        this.progressService = progressService;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
        this.researchChangeRepository = researchChangeRepository;
    }

    public ProjectDashboardService(
            ProjectService projectService,
            ProjectMemberService projectMemberService,
            MilestoneService milestoneService,
            MeetingService meetingService,
            TaskService taskService,
            TaskSuggestionService taskSuggestionService,
            ProgressService progressService) {
        this(
            projectService,
            projectMemberService,
            milestoneService,
            meetingService,
            taskService,
            taskSuggestionService,
            progressService,
            null,
            null,
            null,
            null,
            null);
    }

    public ProjectDashboard getDashboard(Long projectId) {
        Project project = projectService.getProjectById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        List<ProjectMember> members = projectMemberService.getMembersByProject(projectId);
        List<Milestone> milestones = milestoneService.getMilestonesByProject(projectId);
        List<Meeting> meetings = meetingService.getMeetingsByProject(projectId);

        ResearchMetrics researchMetrics = computeResearchMetrics(projectId);

        return new ProjectDashboard(
            project,
            progressService.getProjectProgress(projectId),
            members,
            studentProgress(projectId, members),
            milestones,
            milestoneProgress(projectId, milestones),
            meetings,
            taskService.getTasksByProject(projectId),
            pendingSuggestions(meetings),
            researchMetrics);
    }

    private ResearchMetrics computeResearchMetrics(Long projectId) {
        if (researchDecisionRepository == null) {
            return null;
        }

        long totalDecisions = researchDecisionRepository.countByProjectId(projectId);
        long activeDecisions = researchDecisionRepository.countByProjectIdAndStatus(
            projectId,
            ResearchDecisionStatus.ACTIVE);
        long totalExperiments = experimentRepository != null ? experimentRepository.countByProjectId(projectId) : 0;
        long totalResults = resultRepository != null ? resultRepository.countByProjectId(projectId) : 0;
        long totalEvents = researchEventRepository != null ? researchEventRepository.countByProjectId(projectId) : 0;
        long totalChanges = researchChangeRepository != null ? researchChangeRepository.countByProjectId(projectId) : 0;

        return new ResearchMetrics(
            totalDecisions,
            activeDecisions,
            totalExperiments,
            totalResults,
            totalEvents,
            totalChanges);
    }

    private List<StudentProgressItem> studentProgress(Long projectId, List<ProjectMember> members) {
        return members.stream()
            .filter(member -> member.getRole() == ProjectMemberRole.STUDENT)
            .map(ProjectMember::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .map(studentId -> new StudentProgressItem(
                studentId,
                progressService.getStudentProgress(studentId, projectId)))
            .toList();
    }

    private List<MilestoneProgressItem> milestoneProgress(Long projectId, List<Milestone> milestones) {
        return milestones.stream()
            .map(milestone -> new MilestoneProgressItem(
                milestone.getId(),
                progressService.getMilestoneProgress(milestone.getId(), milestone.getProjectId() != null ? milestone.getProjectId() : projectId)))
            .toList();
    }

    private List<TaskSuggestion> pendingSuggestions(List<Meeting> meetings) {
        List<TaskSuggestion> pending = new ArrayList<>();
        for (Meeting meeting : meetings) {
            if (meeting.getId() == null) {
                continue;
            }
            for (TaskSuggestion suggestion : taskSuggestionService.listByMeeting(meeting.getId())) {
                if (suggestion.getStatus() == TaskSuggestionStatus.PENDING) {
                    pending.add(suggestion);
                }
            }
        }
        return List.copyOf(pending);
    }
}
