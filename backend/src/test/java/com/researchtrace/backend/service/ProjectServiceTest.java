package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.ProjectWriteRequest;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ProjectService.class)
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void createsAProjectWithObjectivesDatesAndSupervisor() {
        User supervisor = user(UserRole.PROFESSOR, "Prof. Pulkit Verma");
        ProjectWriteRequest request = profile("Multimodal Biomedical AI");
        request.setDescription("Pathology vision models");
        request.setObjectives("Improve slide-level classification");
        request.setStartDate(LocalDate.of(2026, 1, 6));
        request.setTargetDate(LocalDate.of(2026, 12, 15));
        request.setSupervisorId(supervisor.getId());

        Project created = projectService.createProject(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(created.getObjectives()).isEqualTo("Improve slide-level classification");
        assertThat(created.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(created.getTargetDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(created.getSupervisorId()).isEqualTo(supervisor.getId());
    }

    @Test
    void leavesNewProfileFieldsNullWhenTheyAreNotSupplied() {
        Project created = projectService.createProject(profile("GreenRoute"));

        assertThat(created.getDescription()).isNull();
        assertThat(created.getObjectives()).isNull();
        assertThat(created.getStartDate()).isNull();
        assertThat(created.getTargetDate()).isNull();
        assertThat(created.getSupervisorId()).isNull();
    }

    @Test
    void loadsAnExistingProjectWhoseNewFieldsAreNull() {
        Project existing = new Project();
        existing.setName("Legacy project");
        existing.setStatus(ProjectStatus.ACTIVE);
        Long id = projectRepository.save(existing).getId();
        entityManager.flush();
        entityManager.clear();

        Project loaded = projectService.getProjectById(id).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Legacy project");
        assertThat(loaded.getObjectives()).isNull();
        assertThat(loaded.getStartDate()).isNull();
        assertThat(loaded.getTargetDate()).isNull();
        assertThat(loaded.getSupervisorId()).isNull();
    }

    @Test
    void rejectsABlankName() {
        ProjectWriteRequest request = profile("   ");

        assertThatThrownBy(() -> projectService.createProject(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Project name is required");
    }

    @Test
    void rejectsAStartDateAfterTheTargetDate() {
        ProjectWriteRequest request = profile("Crop Disease Detection");
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setTargetDate(LocalDate.of(2026, 7, 1));

        assertThatThrownBy(() -> projectService.createProject(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Start date must not be later than the target completion date");
    }

    @Test
    void rejectsAnUnknownSupervisor() {
        ProjectWriteRequest request = profile("Crop Disease Detection");
        request.setSupervisorId(404L);

        assertThatThrownBy(() -> projectService.createProject(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Supervisor not found");
    }

    @Test
    void updatesTheExistingProjectInsteadOfCreatingAnother() {
        Project created = projectService.createProject(profile("GreenRoute"));
        ProjectWriteRequest update = profile("GreenRoute routing study");
        update.setObjectives("Reduce urban emissions");
        update.setStartDate(LocalDate.of(2026, 2, 1));

        Project updated = projectService.updateProject(created.getId(), update);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(updated.getObjectives()).isEqualTo("Reduce urban emissions");
        assertThat(projectRepository.findAll()).hasSize(1);
    }

    @Test
    void archiveKeepsTheProjectAndItsMeeting() {
        Project created = projectService.createProject(profile("Multimodal Biomedical AI"));
        Meeting meeting = new Meeting();
        meeting.setProjectId(created.getId());
        meeting.setTitle("ViT-B16 review");
        meeting.setMeetingDate(LocalDate.of(2026, 11, 2));
        Long meetingId = meetingRepository.save(meeting).getId();

        Project archived = projectService.archiveProject(created.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(archived.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(projectRepository.findById(created.getId())).isPresent();
        assertThat(meetingRepository.findById(meetingId)).isPresent();
        assertThat(projectService.getAllProjects())
            .extracting(Project::getStatus)
            .contains(ProjectStatus.ARCHIVED);
    }

    @Test
    void deleteIsRejectedWhenChildRecordsExist() {
        Project created = projectService.createProject(profile("GreenRoute"));
        Meeting meeting = new Meeting();
        meeting.setProjectId(created.getId());
        meeting.setTitle("Route planning");
        meeting.setMeetingDate(LocalDate.of(2026, 3, 1));
        meetingRepository.save(meeting);

        assertThatThrownBy(() -> projectService.deleteProject(created.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Archive the project");
        assertThat(projectRepository.findById(created.getId())).isPresent();
        assertThat(meetingRepository.findByProjectId(created.getId())).hasSize(1);
    }

    @Test
    void listsProjectsAfterCreation() {
        projectService.createProject(profile("Project A"));
        projectService.createProject(profile("Project B"));

        assertThat(projectService.getAllProjects()).extracting(Project::getName)
            .contains("Project A", "Project B");
    }

    private ProjectWriteRequest profile(String name) {
        ProjectWriteRequest request = new ProjectWriteRequest();
        request.setName(name);
        return request;
    }

    private User user(UserRole role, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase().replace(" ", ".") + "@research.edu");
        user.setRole(role);
        return userRepository.save(user);
    }
}
