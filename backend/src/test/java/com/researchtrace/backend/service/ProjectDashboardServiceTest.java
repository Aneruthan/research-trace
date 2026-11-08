package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
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

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private ResearchEventRepository researchEventRepository;

    @Mock
    private ResearchChangeRepository researchChangeRepository;

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
        when(progressService.getMilestoneProgress(11L, PROJECT_ID)).thenReturn(new BigDecimal("50.00"));
        when(progressService.getStudentProgress(7L, PROJECT_ID)).thenReturn(new BigDecimal("100.00"));

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
        assertThat(dashboard.researchMetrics()).isNull();
        verify(progressService).getMilestoneProgress(11L, PROJECT_ID);
        verify(progressService).getProjectProgress(PROJECT_ID);
    }

    @Test
    void assemblesDashboardWithResearchMetricsWhenRepositoriesProvided() {
        ProjectDashboardService fullDashboardService = new ProjectDashboardService(
            projectService,
            projectMemberService,
            milestoneService,
            meetingService,
            taskService,
            taskSuggestionService,
            progressService,
            researchDecisionRepository,
            experimentRepository,
            resultRepository,
            researchEventRepository,
            researchChangeRepository);

        Project project = project();
        when(projectService.getProjectById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMemberService.getMembersByProject(PROJECT_ID)).thenReturn(List.of());
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());
        when(meetingService.getMeetingsByProject(PROJECT_ID)).thenReturn(List.of());
        when(taskService.getTasksByProject(PROJECT_ID)).thenReturn(List.of());
        when(progressService.getProjectProgress(PROJECT_ID)).thenReturn(new BigDecimal("0.00"));

        when(researchDecisionRepository.countByProjectId(PROJECT_ID)).thenReturn(5L);
        when(researchDecisionRepository.countByProjectIdAndStatus(PROJECT_ID, ResearchDecisionStatus.ACTIVE)).thenReturn(3L);
        when(experimentRepository.countByProjectId(PROJECT_ID)).thenReturn(4L);
        when(resultRepository.countByProjectId(PROJECT_ID)).thenReturn(6L);
        when(researchEventRepository.countByProjectId(PROJECT_ID)).thenReturn(12L);
        when(researchChangeRepository.countByProjectId(PROJECT_ID)).thenReturn(2L);

        ProjectDashboard dashboard = fullDashboardService.getDashboard(PROJECT_ID);

        assertThat(dashboard.researchMetrics()).isNotNull();
        assertThat(dashboard.researchMetrics().totalDecisions()).isEqualTo(5L);
        assertThat(dashboard.researchMetrics().activeDecisions()).isEqualTo(3L);
        assertThat(dashboard.researchMetrics().totalExperiments()).isEqualTo(4L);
        assertThat(dashboard.researchMetrics().totalResults()).isEqualTo(6L);
        assertThat(dashboard.researchMetrics().totalEvents()).isEqualTo(12L);
        assertThat(dashboard.researchMetrics().totalChanges()).isEqualTo(2L);
    }

    @Test
    void missingProjectIsRejected() {
        when(projectService.getProjectById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboard(PROJECT_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Project not found");
    }

    @Test
    void studentProgressIsScopedToRequestedProjectWhenStudentInMultipleProjects() {
        Long projectOneId = 1L;
        Long projectTwoId = 2L;
        Long studentId = 7L;

        Project projectOne = new Project();
        projectOne.setId(projectOneId);
        projectOne.setName("Project Alpha");

        Project projectTwo = new Project();
        projectTwo.setId(projectTwoId);
        projectTwo.setName("Project Beta");

        ProjectMember memberOne = new ProjectMember();
        memberOne.setProjectId(projectOneId);
        memberOne.setUserId(studentId);
        memberOne.setRole(ProjectMemberRole.STUDENT);

        ProjectMember memberTwo = new ProjectMember();
        memberTwo.setProjectId(projectTwoId);
        memberTwo.setUserId(studentId);
        memberTwo.setRole(ProjectMemberRole.STUDENT);

        when(projectService.getProjectById(projectOneId)).thenReturn(Optional.of(projectOne));
        when(projectService.getProjectById(projectTwoId)).thenReturn(Optional.of(projectTwo));
        when(projectMemberService.getMembersByProject(projectOneId)).thenReturn(List.of(memberOne));
        when(projectMemberService.getMembersByProject(projectTwoId)).thenReturn(List.of(memberTwo));

        when(milestoneService.getMilestonesByProject(projectOneId)).thenReturn(List.of());
        when(milestoneService.getMilestonesByProject(projectTwoId)).thenReturn(List.of());
        when(meetingService.getMeetingsByProject(projectOneId)).thenReturn(List.of());
        when(meetingService.getMeetingsByProject(projectTwoId)).thenReturn(List.of());
        when(taskService.getTasksByProject(projectOneId)).thenReturn(List.of());
        when(taskService.getTasksByProject(projectTwoId)).thenReturn(List.of());

        when(progressService.getProjectProgress(projectOneId)).thenReturn(new BigDecimal("100.00"));
        when(progressService.getProjectProgress(projectTwoId)).thenReturn(new BigDecimal("25.00"));

        when(progressService.getStudentProgress(studentId, projectOneId)).thenReturn(new BigDecimal("100.00"));
        when(progressService.getStudentProgress(studentId, projectTwoId)).thenReturn(new BigDecimal("25.00"));

        ProjectDashboard dashboardOne = dashboardService.getDashboard(projectOneId);
        ProjectDashboard dashboardTwo = dashboardService.getDashboard(projectTwoId);

        assertThat(dashboardOne.studentProgress()).containsExactly(
            new ProjectDashboard.StudentProgressItem(studentId, new BigDecimal("100.00")));
        assertThat(dashboardTwo.studentProgress()).containsExactly(
            new ProjectDashboard.StudentProgressItem(studentId, new BigDecimal("25.00")));

        verify(progressService).getStudentProgress(studentId, projectOneId);
        verify(progressService).getStudentProgress(studentId, projectTwoId);
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
