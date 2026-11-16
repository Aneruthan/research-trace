package com.researchtrace.backend.service;

import com.researchtrace.backend.model.User;
import com.researchtrace.backend.model.UserRole;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskRepository taskRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createsAStudentAndRejectsADuplicateEmail() {
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        User created = userService.createUser(request(" Alex Johnson ", "alex@example.com", UserRole.STUDENT));
        assertThat(created.getId()).isEqualTo(5L);
        assertThat(created.getName()).isEqualTo("Alex Johnson");
        assertThat(created.getRole()).isEqualTo(UserRole.STUDENT);

        User existing = new User();
        existing.setId(5L);
        existing.setEmail("alex@example.com");
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> userService.createUser(request("Other", "Alex@example.com", UserRole.STUDENT)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updatesTheSameUserWithoutCreatingAnotherRecord() {
        User existing = new User();
        existing.setId(5L);
        existing.setName("Alex");
        existing.setEmail("alex@example.com");
        existing.setRole(UserRole.STUDENT);
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("alex.johnson@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(5L, request("Alex Johnson", "alex.johnson@example.com", UserRole.STUDENT));

        assertThat(updated.getId()).isEqualTo(5L);
        assertThat(updated.getName()).isEqualTo("Alex Johnson");
        verify(userRepository).save(existing);
    }

    @Test
    void deleteRefusesToRemoveAUserWhoStillHasAMembershipOrAssignedTasks() {
        UserService guarded = new UserService(userRepository, projectMemberRepository, taskRepository);
        when(projectMemberRepository.findByUserId(5L)).thenReturn(List.of(new com.researchtrace.backend.model.ProjectMember()));

        assertThatThrownBy(() -> guarded.deleteUser(5L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).deleteById(5L);
    }

    @Test
    void rejectsABlankName() {
        assertThatThrownBy(() -> userService.createUser(request(" ", "alex@example.com", UserRole.STUDENT)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userRepository, never()).save(any());
    }

    private static User request(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
