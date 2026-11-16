package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoSeedServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Test
    void existingProjectAndUserAreNotOverwritten() {
        Project project = new Project();
        project.setId(4L);
        project.setName("GreenRoute");
        User user = new User();
        user.setId(2L);
        user.setEmail("alex@research.edu");
        when(projectRepository.findAll()).thenReturn(List.of(project));
        when(userRepository.findByEmailIgnoreCase("alex@research.edu")).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectId(4L)).thenReturn(List.of());

        new DemoSeedService(projectRepository, userRepository, projectMemberRepository).seedMissing();

        verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void demoSeedIsOffUnlessExplicitlyEnabled() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);
        assertThat(properties).contains("researchtrace.demo.seed=false");
        assertThat(properties).contains("server.error.include-stacktrace=never");
    }
}
