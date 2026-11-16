package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.StudentWorkView;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.UserRepository;
import com.researchtrace.backend.service.TaskWork.Counts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class StudentWorkService {

    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final ProgressService progressService;

    public StudentWorkService(
            ProjectService projectService,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            TaskService taskService,
            ProgressService progressService) {
        this.projectService = projectService;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.progressService = progressService;
    }

    public List<StudentWorkView> listStudents(Long projectId) {
        requireProject(projectId);
        List<Task> tasks = taskService.getTasksByProject(projectId);
        return projectMemberRepository.findByProjectId(projectId).stream()
            .filter(member -> member.getRole() == ProjectMemberRole.STUDENT)
            .map(member -> toView(projectId, member, tasks, false))
            .sorted(Comparator.comparing(StudentWorkView::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    public StudentWorkView getStudent(Long projectId, Long studentId) {
        requireProject(projectId);
        ProjectMember member = projectMemberRepository.findByProjectId(projectId).stream()
            .filter(candidate -> studentId != null && studentId.equals(candidate.getUserId()))
            .filter(candidate -> candidate.getRole() == ProjectMemberRole.STUDENT)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        List<Task> tasks = taskService.getTasksByProject(projectId);
        return toView(projectId, member, tasks, true);
    }

    private StudentWorkView toView(Long projectId, ProjectMember member, List<Task> projectTasks, boolean includeTasks) {
        List<Task> assigned = projectTasks.stream()
            .filter(task -> member.getUserId() != null && member.getUserId().equals(task.getAssigneeId()))
            .toList();
        Counts counts = TaskWork.count(assigned, LocalDate.now());
        User user = member.getUserId() == null ? null : userRepository.findById(member.getUserId()).orElse(null);
        StudentWorkView view = new StudentWorkView();
        view.setUserId(member.getUserId());
        view.setMembershipId(member.getId());
        view.setProjectId(projectId);
        view.setName(user == null ? null : user.getName());
        view.setEmail(user == null ? null : user.getEmail());
        view.setRole(member.getRole() == null ? null : member.getRole().name());
        view.setTotalTasks(counts.total());
        view.setCompletedTasks(counts.completed());
        view.setInProgressTasks(counts.inProgress());
        view.setPendingTasks(counts.pending());
        view.setOverdueTasks(counts.overdue());
        view.setProgress(progressService.calculateTaskProgress(assigned));
        view.setProjectMembershipCount(member.getUserId() == null
            ? 0
            : projectMemberRepository.findByUserId(member.getUserId()).size());
        if (includeTasks) {
            view.setTasks(assigned);
        }
        return view;
    }

    private void requireProject(Long projectId) {
        if (projectService.getProjectById(projectId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }
}
