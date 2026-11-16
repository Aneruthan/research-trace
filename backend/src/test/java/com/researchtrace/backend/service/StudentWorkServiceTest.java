package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.StudentWorkView;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentWorkServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskService taskService;

    private final ProgressService progressService = new ProgressService(null, null);

    private StudentWorkService studentWorkService;

    @BeforeEach
    void setUp() {
        studentWorkService = new StudentWorkService(
            projectService, projectMemberRepository, userRepository, taskService, progressService);
    }

    @Test
    void listsOnlyStudentMembersAndIgnoresAnotherStudentsTasks() {
        when(projectService.getProjectById(1L)).thenReturn(Optional.of(new Project()));
        ProjectMember alex = member(4L, 7L, ProjectMemberRole.STUDENT);
        ProjectMember professor = member(5L, 2L, ProjectMemberRole.PROFESSOR);
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of(professor, alex));
        User user = new User();
        user.setId(7L);
        user.setName("Alex Johnson");
        user.setEmail("alex@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByUserId(7L)).thenReturn(List.of(alex, member(9L, 7L, ProjectMemberRole.STUDENT)));
        when(taskService.getTasksByProject(1L)).thenReturn(List.of(
            task(1L, 7L, TaskStatus.COMPLETED, LocalDate.now().minusDays(2)),
            task(2L, 7L, TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(1)),
            task(3L, 7L, TaskStatus.TODO, LocalDate.now().plusDays(1)),
            task(4L, 8L, TaskStatus.TODO, LocalDate.now().minusDays(4))
        ));

        List<StudentWorkView> students = studentWorkService.listStudents(1L);

        assertThat(students).hasSize(1);
        StudentWorkView alexView = students.get(0);
        assertThat(alexView.getName()).isEqualTo("Alex Johnson");
        assertThat(alexView.getEmail()).isEqualTo("alex@example.com");
        assertThat(alexView.getTotalTasks()).isEqualTo(3);
        assertThat(alexView.getCompletedTasks()).isEqualTo(1);
        assertThat(alexView.getInProgressTasks()).isEqualTo(1);
        assertThat(alexView.getPendingTasks()).isEqualTo(1);
        assertThat(alexView.getOverdueTasks()).isEqualTo(1);
        assertThat(alexView.getProgress()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(alexView.getProjectMembershipCount()).isEqualTo(2);
        assertThat(alexView.getTasks()).isNull();
    }

    @Test
    void detailIsNotFoundForAUserWhoIsNotAStudentMember() {
        when(projectService.getProjectById(1L)).thenReturn(Optional.of(new Project()));
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentWorkService.getStudent(1L, 7L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void aStudentWithNoTasksHasZeroProgress() {
        when(projectService.getProjectById(1L)).thenReturn(Optional.of(new Project()));
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of(member(4L, 7L, ProjectMemberRole.STUDENT)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
        when(projectMemberRepository.findByUserId(7L)).thenReturn(List.of(member(4L, 7L, ProjectMemberRole.STUDENT)));
        when(taskService.getTasksByProject(1L)).thenReturn(List.of());

        StudentWorkView student = studentWorkService.getStudent(1L, 7L);

        assertThat(student.getTotalTasks()).isZero();
        assertThat(student.getProgress()).isEqualByComparingTo("0.00");
        assertThat(student.getTasks()).isEmpty();
    }

    @Test
    void reportsATaskAssigneeWhoIsMissingFromTheStudentListAndCanRepairAStudent() {
        when(projectService.getProjectById(1L)).thenReturn(Optional.of(new Project()));
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of(member(4L, 7L, ProjectMemberRole.STUDENT)));
        when(taskService.getTasksByProject(1L)).thenReturn(List.of(task(4L, 8L, TaskStatus.TODO, LocalDate.now())));
        User priya = new User();
        priya.setId(8L);
        priya.setName("Priya Sharma");
        priya.setRole(UserRole.STUDENT);
        when(userRepository.findById(8L)).thenReturn(Optional.of(priya));

        var gaps = studentWorkService.membershipGaps(1L);

        assertThat(gaps).hasSize(1);
        assertThat(gaps.get(0).getUserId()).isEqualTo(8L);
        assertThat(gaps.get(0).getName()).isEqualTo("Priya Sharma");
        assertThat(gaps.get(0).isRepairable()).isTrue();
        assertThat(gaps.get(0).getTaskIds()).containsExactly(4L);

        when(projectMemberRepository.save(org.mockito.ArgumentMatchers.any(ProjectMember.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        ProjectMember repaired = studentWorkService.repairStudentMembership(1L, 8L);
        assertThat(repaired.getRole()).isEqualTo(ProjectMemberRole.STUDENT);
        assertThat(repaired.getUserId()).isEqualTo(8L);
        verify(projectMemberRepository).save(org.mockito.ArgumentMatchers.any(ProjectMember.class));
    }

    @Test
    void doesNotTurnANonStudentIntoAProjectStudent() {
        when(projectService.getProjectById(1L)).thenReturn(Optional.of(new Project()));
        User professor = new User();
        professor.setId(2L);
        professor.setRole(UserRole.PROFESSOR);
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> studentWorkService.repairStudentMembership(1L, 2L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private static ProjectMember member(Long id, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private static Task task(Long id, Long assigneeId, TaskStatus status, LocalDate dueDate) {
        Task task = new Task();
        task.setId(id);
        task.setProjectId(1L);
        task.setAssigneeId(assigneeId);
        task.setStatus(status);
        task.setDueDate(dueDate);
        return task;
    }
}
