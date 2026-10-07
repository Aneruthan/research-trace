package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDashboardServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private MeetingService meetingService;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskSuggestionService taskSuggestionService;

    @Mock
    private ProgressService progressService;

    private ProjectDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new ProjectDashboardService(
            projectService,
            projectMemberService,
            milestoneService,
            meetingService,
            taskService,
            taskSuggestionService,
            progressService);
    }

    @Test
    void assemblesReadOnlyDashboardFromExistingServices() {
        Project project = project();
        ProjectMember student = member(7L, ProjectMemberRole.STUDENT);
        ProjectMember professor = member(3L, ProjectMemberRole.PROFESSOR);
        Milestone milestone = milestone(11L);
        Meeting meeting = meeting(21L);
        Task task = task(500L);
        TaskSuggestion pending = suggestion(40L, TaskSuggestionStatus.PENDING);
        TaskSuggestion accepted = suggestion(41L, TaskSuggestionStatus.ACCEPTED);

        when(projectService.getProjectById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberService.getMembersByProject(PROJECT_ID)).thenReturn(List.of(student, professor));
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone));
        when(meetingService.getMeetingsByProject(PROJECT_ID)).thenReturn(List.of(meeting));
        when(taskService.getTasksByProject(PROJECT_ID)).thenReturn(List.of(task));
        when(taskSuggestionService.listByMeeting(21L)).thenReturn(List.of(accepted, pending));
        when(progressService.getProjectProgress(PROJECT_ID)).thenReturn(new BigDecimal("50.00"));
        when(progressService.getMilestoneProgress(11L)).thenReturn(new BigDecimal("50.00"));
        when(progressService.getStudentProgress(7L)).thenReturn(new BigDecimal("100.00"));

        ProjectDashboard dashboard = dashboardService.getDashboard(PROJECT_ID);

        assertThat(dashboard.project()).isSameAs(project);
        assertThat(dashboard.projectProgress()).isEqualByComparingTo("50.00");
        assertThat(dashboard.members()).containsExactly(student, professor);
        assertThat(dashboard.studentProgress()).containsExactly(
            new ProjectDashboard.StudentProgressItem(7L, new BigDecimal("100.00")));
        assertThat(dashboard.milestones()).containsExactly(milestone);
        assertThat(dashboard.milestoneProgress()).containsExactly(
            new ProjectDashboard.MilestoneProgressItem(11L, new BigDecimal("50.00")));
        assertThat(dashboard.meetings()).containsExactly(meeting);
        assertThat(dashboard.tasks()).containsExactly(task);
        assertThat(dashboard.pendingTaskSuggestions()).containsExactly(pending);
        verify(progressService).getMilestoneProgress(11L);
        verify(progressService).getProjectProgress(PROJECT_ID);
    }

    @Test
    void missingProjectIsRejected() {
        when(projectService.getProjectById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboard(PROJECT_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Project not found");
    }

    private static Project project() {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setName("ResearchTrace");
        project.setStatus(ProjectStatus.ACTIVE);
        return project;
    }

    private static ProjectMember member(Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(PROJECT_ID);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private static Milestone milestone(Long id) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProjectId(PROJECT_ID);
        milestone.setTitle("Baseline evaluation");
        return milestone;
    }

    private static Meeting meeting(Long id) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setProjectId(PROJECT_ID);
        meeting.setTitle("Weekly sync");
        return meeting;
    }

    private static Task task(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Run Qwen baseline");
        task.setStatus(TaskStatus.TODO);
        return task;
    }

    private static TaskSuggestion suggestion(Long id, TaskSuggestionStatus status) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setId(id);
        suggestion.setStatus(status);
        suggestion.setTitle("Suggestion " + id);
        return suggestion;
    }
}
