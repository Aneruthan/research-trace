package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.ProjectDashboard;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.service.MeetingService;
import com.researchtrace.backend.service.MilestoneService;
import com.researchtrace.backend.service.ProgressService;
import com.researchtrace.backend.service.ProjectDashboardService;
import com.researchtrace.backend.service.ProjectMemberService;
import com.researchtrace.backend.service.ProjectService;
import com.researchtrace.backend.service.ResearchQueryService;
import com.researchtrace.backend.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProgressService progressService;

    @MockitoBean
    private MilestoneService milestoneService;

    @MockitoBean
    private ProjectMemberService projectMemberService;

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectDashboardService projectDashboardService;

    @MockitoBean
    private ResearchQueryService researchQueryService;

    @Test
    void returnsProjectProgress() throws Exception {
        when(progressService.getProjectProgress(1L)).thenReturn(new BigDecimal("70.00"));

        mockMvc.perform(get("/api/projects/1/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("70.00"));
    }

    @Test
    void returnsZeroProgressForProjectWithoutMilestones() throws Exception {
        when(progressService.getProjectProgress(9L)).thenReturn(new BigDecimal("0.00"));

        mockMvc.perform(get("/api/projects/9/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("0.00"));
    }

    @Test
    void stillServesTheExistingProjectEndpoint() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setName("ResearchTrace");
        project.setStatus(ProjectStatus.ACTIVE);

        when(projectService.getProjectById(1L)).thenReturn(Optional.of(project));

        mockMvc.perform(get("/api/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("ResearchTrace"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnsMilestonesForProject() throws Exception {
        Milestone milestone = new Milestone();
        milestone.setId(11L);
        milestone.setProjectId(1L);
        milestone.setTitle("Baseline evaluation");
        when(milestoneService.getMilestonesByProject(1L)).thenReturn(List.of(milestone));

        mockMvc.perform(get("/api/projects/1/milestones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].title").value("Baseline evaluation"));
    }

    @Test
    void returnsMembersForProject() throws Exception {
        ProjectMember member = new ProjectMember();
        member.setId(4L);
        member.setProjectId(1L);
        member.setUserId(7L);
        member.setRole(ProjectMemberRole.STUDENT);
        when(projectMemberService.getMembersByProject(1L)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/projects/1/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(7))
            .andExpect(jsonPath("$[0].role").value("STUDENT"));
    }

    @Test
    void returnsMeetingsForProject() throws Exception {
        Meeting meeting = new Meeting();
        meeting.setId(21L);
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        when(meetingService.getMeetingsByProject(1L)).thenReturn(List.of(meeting));

        mockMvc.perform(get("/api/projects/1/meetings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Weekly sync"));
    }

    @Test
    void returnsTasksForProject() throws Exception {
        Task task = new Task();
        task.setId(500L);
        task.setTitle("Run Qwen baseline");
        task.setStatus(TaskStatus.TODO);
        when(taskService.getTasksByProject(1L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/projects/1/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(500))
            .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void returnsProjectDashboard() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setName("ResearchTrace");
        when(projectDashboardService.getDashboard(1L)).thenReturn(new ProjectDashboard(
            project,
            new BigDecimal("50.00"),
            List.of(),
            List.of(new ProjectDashboard.StudentProgressItem(7L, new BigDecimal("50.00"))),
            List.of(),
            List.of(new ProjectDashboard.MilestoneProgressItem(11L, new BigDecimal("50.00"))),
            List.of(),
            List.of(),
            List.of()));

        mockMvc.perform(get("/api/projects/1/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.project.name").value("ResearchTrace"))
            .andExpect(jsonPath("$.projectProgress").value(50.00))
            .andExpect(jsonPath("$.studentProgress[0].studentId").value(7))
            .andExpect(jsonPath("$.milestoneProgress[0].milestoneId").value(11))
            .andExpect(jsonPath("$.pendingTaskSuggestions").isArray());
    }

    @Test
    void returnsResearchRecordsForTheRequestedProject() throws Exception {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(3L);
        decision.setProjectId(1L);
        decision.setTitle("Keep the baseline");
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        Experiment experiment = new Experiment();
        experiment.setId(8L);
        experiment.setProjectId(1L);
        experiment.setTitle("Bias audit");
        Result result = new Result();
        result.setId(9L);
        result.setProjectId(1L);
        result.setSummary("Gap is 4 points");
        ResearchEvent earlier = new ResearchEvent();
        earlier.setId(1L);
        earlier.setTitle("Meeting held");
        earlier.setEventType(ResearchEventType.MEETING);
        ResearchEvent later = new ResearchEvent();
        later.setId(2L);
        later.setTitle("Decision recorded");
        later.setEventType(ResearchEventType.DECISION);
        when(researchQueryService.getResearchDecisions(1L)).thenReturn(List.of(decision));
        when(researchQueryService.getExperiments(1L)).thenReturn(List.of(experiment));
        when(researchQueryService.getResults(1L)).thenReturn(List.of(result));
        when(researchQueryService.getResearchEvents(1L)).thenReturn(List.of(earlier, later));
        when(researchQueryService.getResearchDecisions(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/1/research-decisions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Keep the baseline"))
            .andExpect(jsonPath("$[0].projectId").value(1));
        mockMvc.perform(get("/api/projects/1/experiments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bias audit"));
        mockMvc.perform(get("/api/projects/1/results"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].summary").value("Gap is 4 points"));
        mockMvc.perform(get("/api/projects/1/research-events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Meeting held"))
            .andExpect(jsonPath("$[1].title").value("Decision recorded"));
        mockMvc.perform(get("/api/projects/2/research-decisions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
