package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.dto.ProjectDashboard.MilestoneProgressItem;
import com.researchtrace.backend.dto.ProjectDashboard.StudentProgressItem;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
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

    public ProjectDashboardService(
            ProjectService projectService,
            ProjectMemberService projectMemberService,
            MilestoneService milestoneService,
            MeetingService meetingService,
            TaskService taskService,
            TaskSuggestionService taskSuggestionService,
            ProgressService progressService) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.milestoneService = milestoneService;
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.taskSuggestionService = taskSuggestionService;
        this.progressService = progressService;
    }

    public ProjectDashboard getDashboard(Long projectId) {
        Project project = projectService.getProjectById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        List<ProjectMember> members = projectMemberService.getMembersByProject(projectId);
        List<Milestone> milestones = milestoneService.getMilestonesByProject(projectId);
        List<Meeting> meetings = meetingService.getMeetingsByProject(projectId);

        return new ProjectDashboard(
            project,
            progressService.getProjectProgress(projectId),
            members,
            studentProgress(members),
            milestones,
            milestoneProgress(milestones),
            meetings,
            taskService.getTasksByProject(projectId),
            pendingSuggestions(meetings));
    }

    private List<StudentProgressItem> studentProgress(List<ProjectMember> members) {
        return members.stream()
            .filter(member -> member.getRole() == ProjectMemberRole.STUDENT)
            .map(ProjectMember::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .map(studentId -> new StudentProgressItem(
                studentId,
                progressService.getStudentProgress(studentId)))
            .toList();
    }

    private List<MilestoneProgressItem> milestoneProgress(List<Milestone> milestones) {
        return milestones.stream()
            .map(milestone -> new MilestoneProgressItem(
                milestone.getId(),
                progressService.getMilestoneProgress(milestone.getId())))
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
