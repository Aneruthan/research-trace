package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.ProjectStatus;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Adds missing demo rows only. Existing projects, users, and memberships are left unchanged.
 * The runner is created only when {@code researchtrace.demo.seed=true}.
 */
@Service
public class DemoSeedService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public DemoSeedService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public void seedMissing() {
        Project project = projectRepository.findAll().stream()
            .filter(existing -> "GreenRoute".equals(existing.getName()))
            .findFirst()
            .orElseGet(this::createProject);
        User student = userRepository.findByEmailIgnoreCase("alex@research.edu")
            .orElseGet(this::createStudent);
        boolean alreadyMember = project.getId() != null && projectMemberRepository.findByProjectId(project.getId()).stream()
            .anyMatch(member -> student.getId() != null && student.getId().equals(member.getUserId()));
        if (!alreadyMember && project.getId() != null && student.getId() != null) {
            ProjectMember membership = new ProjectMember();
            membership.setProjectId(project.getId());
            membership.setUserId(student.getId());
            membership.setRole(ProjectMemberRole.STUDENT);
            projectMemberRepository.save(membership);
        }
    }

    private Project createProject() {
        Project project = new Project();
        project.setName("GreenRoute");
        project.setDescription("Opt-in demo project. Existing projects are not modified.");
        project.setStatus(ProjectStatus.ACTIVE);
        return projectRepository.save(project);
    }

    private User createStudent() {
        User user = new User();
        user.setName("Alex Johnson");
        user.setEmail("alex@research.edu");
        user.setRole(UserRole.STUDENT);
        return userRepository.save(user);
    }

    @Component
    @ConditionalOnProperty(name = "researchtrace.demo.seed", havingValue = "true")
    static class Runner implements ApplicationRunner {
        private final DemoSeedService demoSeedService;

        Runner(DemoSeedService demoSeedService) {
            this.demoSeedService = demoSeedService;
        }

        @Override
        public void run(ApplicationArguments args) {
            demoSeedService.seedMissing();
        }
    }
}
