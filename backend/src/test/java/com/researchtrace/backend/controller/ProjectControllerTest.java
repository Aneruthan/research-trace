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
import com.researchtrace.backend.model.TaskHistory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void createsAProject() throws Exception {
        Project project = new Project();
        project.setId(4L);
        project.setName("GreenRoute");
        project.setStatus(ProjectStatus.ACTIVE);
        when(projectService.createProject(any())).thenReturn(project);

        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"GreenRoute\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.name").value("GreenRoute"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void archivesAProjectWithoutRemovingIt() throws Exception {
        Project project = new Project();
        project.setId(4L);
        project.setName("GreenRoute");
        project.setStatus(ProjectStatus.ARCHIVED);
        when(projectService.archiveProject(4L)).thenReturn(project);

        mockMvc.perform(post("/api/projects/4/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.name").value("GreenRoute"));
    }

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
    void returnsAMeetingOnlyForItsProject() throws Exception {
        Meeting meeting = new Meeting();
        meeting.setId(21L);
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        when(meetingService.getMeetingForProject(1L, 21L)).thenReturn(meeting);
        when(meetingService.getMeetingForProject(2L, 21L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        mockMvc.perform(get("/api/projects/1/meetings/21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Weekly sync"))
            .andExpect(jsonPath("$.projectId").value(1));
        mockMvc.perform(get("/api/projects/2/meetings/21"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsATaskOnlyForItsProject() throws Exception {
        Task task = new Task();
        task.setId(500L);
        task.setProjectId(1L);
        task.setTitle("Run Qwen baseline");
        task.setStatus(TaskStatus.TODO);
        when(taskService.getTaskForProject(1L, 500L)).thenReturn(task);
        when(taskService.getTaskForProject(2L, 500L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        mockMvc.perform(get("/api/projects/1/tasks/500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(500))
            .andExpect(jsonPath("$.projectId").value(1));
        mockMvc.perform(get("/api/projects/2/tasks/500"))
            .andExpect(status().isNotFound());
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

    @Test
    void unscopedMeetingAndTaskRoutesAreNotAvailable() throws Exception {
        mockMvc.perform(get("/api/meetings")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/meetings/4").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/meetings/4")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/tasks/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/10")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/10/history")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/milestone/3")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/assignee/7")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/research-decision/55")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/experiment/66")).andExpect(status().isNotFound());

        verify(meetingService, never()).getAllMeetings();
        verify(taskService, never()).getAllTasks();
        verify(meetingService, never()).updateMeeting(any(), any());
        verify(taskService, never()).saveTask(any(), any());
    }

    @Test
    void updatesAndDeletesAMeetingOnlyInsideItsProject() throws Exception {
        Meeting meeting = new Meeting();
        meeting.setId(4L);
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        when(meetingService.updateMeeting(eq(1L), eq(4L), any())).thenReturn(meeting);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"))
            .when(meetingService).deleteMeeting(4L, 2L);

        mockMvc.perform(put("/api/projects/1/meetings/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Weekly sync\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(1));
        mockMvc.perform(delete("/api/projects/2/meetings/4"))
            .andExpect(status().isNotFound());
    }

    @Test
    void readsHistoryAndUpdatesATaskOnlyInsideItsProject() throws Exception {
        TaskHistory history = new TaskHistory(10L, TaskStatus.TODO, TaskStatus.IN_PROGRESS, 5L, LocalDateTime.of(2026, 10, 1, 10, 0));
        history.setId(1L);
        when(taskService.getTaskHistoryForProject(1L, 10L)).thenReturn(List.of(history));
        when(taskService.getTaskHistoryForProject(2L, 10L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        Task updated = new Task();
        updated.setId(10L);
        updated.setProjectId(1L);
        updated.setTitle("Updated task");
        updated.setStatus(TaskStatus.COMPLETED);
        when(taskService.updateTaskForProject(eq(1L), eq(10L), any(), eq(42L))).thenReturn(updated);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"))
            .when(taskService).deleteTaskForProject(2L, 10L);

        mockMvc.perform(get("/api/projects/1/tasks/10/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/projects/2/tasks/10/history"))
            .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/projects/1/tasks/10?userId=42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated task\",\"status\":\"COMPLETED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(1));
        mockMvc.perform(delete("/api/projects/2/tasks/10"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listsMilestoneTasksOnlyForTheOwningProject() throws Exception {
        Task task = new Task();
        task.setId(15L);
        task.setProjectId(1L);
        task.setTitle("Owned task");
        when(taskService.getTasksByMilestoneForProject(1L, 3L)).thenReturn(List.of(task));
        when(taskService.getTasksByMilestoneForProject(2L, 3L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        mockMvc.perform(get("/api/projects/1/milestones/3/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(15));
        mockMvc.perform(get("/api/projects/2/milestones/3/tasks"))
            .andExpect(status().isNotFound());
    }
}
