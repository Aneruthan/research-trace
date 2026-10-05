package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskSuggestionApiIntegrationTest {

    private static final Long PROJECT_ID = 101L;
    private static final Long OTHER_PROJECT_ID = 102L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSuggestionRepository taskSuggestionRepository;

    private Long meetingId;
    private Long aneruthanId;
    private Long gayathriId;
    private Long outsiderId;
    private Long baselineMilestoneId;
    private Long otherProjectMilestoneId;

    @BeforeEach
    void setUp() {
        aneruthanId = student(PROJECT_ID, "Aneruthan");
        gayathriId = student(PROJECT_ID, "Gayathri");
        outsiderId = student(OTHER_PROJECT_ID, "Outsider");
        baselineMilestoneId = milestone(PROJECT_ID, "Baseline evaluation");
        otherProjectMilestoneId = milestone(OTHER_PROJECT_ID, "Other project milestone");
        meetingId = meeting(PROJECT_ID, """
            ACTION: Aneruthan | Run Qwen baseline | 2026-10-05 | Baseline evaluation
            TODO: Gayathri | Prepare dataset statistics | 2026-10-06
            """);
    }

    @Test
    void extractsListsEditsAcceptsAndRejectsThroughTheApi() throws Exception {
        mockMvc.perform(post("/api/meetings/{id}/task-suggestions/extract", meetingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].assigneeId").value(aneruthanId))
            .andExpect(jsonPath("$[0].milestoneId").value(baselineMilestoneId))
            .andExpect(jsonPath("$[1].assigneeId").value(gayathriId));
        assertThat(taskRepository.count()).isZero();

        List<TaskSuggestion> suggestions = taskSuggestionRepository.findByMeetingId(meetingId);
        Long firstId = idWithTitle(suggestions, "Run Qwen baseline");
        Long secondId = idWithTitle(suggestions, "Prepare dataset statistics");

        mockMvc.perform(get("/api/meetings/{id}/task-suggestions", meetingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(put("/api/task-suggestions/{id}", firstId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"  Run Qwen baseline on dev split  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Run Qwen baseline on dev split"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", firstId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dueDate\": \"2026-10-09\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Run Qwen baseline on dev split"))
            .andExpect(jsonPath("$.assigneeId").value(aneruthanId))
            .andExpect(jsonPath("$.milestoneId").value(baselineMilestoneId))
            .andExpect(jsonPath("$.dueDate").value("2026-10-09"))
            .andExpect(jsonPath("$.status").value(TaskStatus.TODO.name()));

        mockMvc.perform(post("/api/task-suggestions/{id}/reject", secondId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(taskRepository.count()).isEqualTo(1);
        assertThat(taskSuggestionRepository.findById(firstId).orElseThrow().getAcceptedTaskId()).isNotNull();
    }

    @Test
    void rejectsStudentFromAnotherProject() throws Exception {
        Long suggestionId = extractFirstSuggestionId();

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": " + outsiderId + "}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/task-suggestions/{id}", suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": " + outsiderId + "}"))
            .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void rejectsMilestoneFromAnotherProject() throws Exception {
        Long suggestionId = extractFirstSuggestionId();

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"milestoneId\": " + otherProjectMilestoneId + "}"))
            .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        Long suggestionId = extractFirstSuggestionId();

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"   \"}"))
            .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void returnsNotFoundForMissingMeetingAndSuggestion() throws Exception {
        mockMvc.perform(post("/api/meetings/{id}/task-suggestions/extract", 999_999L))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/meetings/{id}/task-suggestions", 999_999L))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/task-suggestions/{id}/accept", 999_999L))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/task-suggestions/{id}/reject", 999_999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictWhenReviewingProcessedSuggestions() throws Exception {
        Long suggestionId = extractFirstSuggestionId();

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", suggestionId))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/task-suggestions/{id}/accept", suggestionId))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/task-suggestions/{id}/reject", suggestionId))
            .andExpect(status().isConflict());
        mockMvc.perform(put("/api/task-suggestions/{id}", suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Changed\"}"))
            .andExpect(status().isConflict());

        assertThat(taskRepository.count()).isEqualTo(1);
    }

    private Long extractFirstSuggestionId() throws Exception {
        mockMvc.perform(post("/api/meetings/{id}/task-suggestions/extract", meetingId))
            .andExpect(status().isOk());
        return idWithTitle(taskSuggestionRepository.findByMeetingId(meetingId), "Run Qwen baseline");
    }

    private Long idWithTitle(List<TaskSuggestion> suggestions, String title) {
        return suggestions.stream()
            .filter(suggestion -> title.equals(suggestion.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();
    }

    private Long student(Long projectId, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setRole(UserRole.STUDENT);
        Long userId = userRepository.save(user).getId();

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(ProjectMemberRole.STUDENT);
        projectMemberRepository.save(member);
        return userId;
    }

    private Long milestone(Long projectId, String title) {
        Milestone milestone = new Milestone();
        milestone.setProjectId(projectId);
        milestone.setTitle(title);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setWeight(new BigDecimal("100"));
        return milestoneRepository.save(milestone).getId();
    }

    private Long meeting(Long projectId, String notes) {
        Meeting meeting = new Meeting();
        meeting.setProjectId(projectId);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 2));
        meeting.setNotes(notes);
        return meetingRepository.save(meeting).getId();
    }
}
