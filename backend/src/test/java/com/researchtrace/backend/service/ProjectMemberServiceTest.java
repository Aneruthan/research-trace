package com.researchtrace.backend.service;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.User;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    private ProjectMemberService projectMemberService;

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(projectMemberRepository, userRepository);
    }

    @Test
    void oneUserCanBelongToTwoProjects() {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of());
        when(projectMemberRepository.findByProjectId(2L)).thenReturn(List.of());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMember request = member(null, 5L, ProjectMemberRole.STUDENT);
        ProjectMember first = projectMemberService.addMember(1L, request);
        ProjectMember second = projectMemberService.addMember(2L, request);

        assertThat(first.getProjectId()).isEqualTo(1L);
        assertThat(second.getProjectId()).isEqualTo(2L);
        assertThat(first.getUserId()).isEqualTo(5L);
        assertThat(second.getUserId()).isEqualTo(5L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void crossProjectMembershipUpdateAndRemovalAreRejected() {
        ProjectMember existing = member(8L, 5L, ProjectMemberRole.STUDENT);
        existing.setProjectId(2L);
        when(projectMemberRepository.findById(8L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> projectMemberService.getMemberForProject(1L, 8L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> projectMemberService.updateMember(1L, 8L, member(null, 5L, ProjectMemberRole.PROFESSOR)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 8L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(projectMemberRepository, never()).save(any());
        verify(projectMemberRepository, never()).deleteById(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void removingMembershipDeletesOnlyTheMembership() {
        ProjectMember existing = member(8L, 5L, ProjectMemberRole.STUDENT);
        existing.setProjectId(1L);
        when(projectMemberRepository.findById(8L)).thenReturn(Optional.of(existing));

        projectMemberService.removeMember(1L, 8L);

        verify(projectMemberRepository).deleteById(8L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updateChangesRoleWithoutMovingTheMembership() {
        ProjectMember existing = member(8L, 5L, ProjectMemberRole.STUDENT);
        existing.setProjectId(1L);
        when(projectMemberRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMember updated = projectMemberService.updateMember(1L, 8L, member(null, null, ProjectMemberRole.PROFESSOR));

        ArgumentCaptor<ProjectMember> saved = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getProjectId()).isEqualTo(1L);
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
        assertThat(updated.getRole()).isEqualTo(ProjectMemberRole.PROFESSOR);
    }

    private static ProjectMember member(Long id, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }
}
