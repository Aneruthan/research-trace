package com.researchtrace.backend.service;

import com.researchtrace.backend.model.User;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository) {
        this(userRepository, null, null);
    }

    @Autowired
    public UserService(
            UserRepository userRepository,
            @Nullable ProjectMemberRepository projectMemberRepository,
            @Nullable TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User request) {
        User user = new User();
        applyProfile(user, request);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User request) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        applyProfile(existing, request);
        return userRepository.save(existing);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (projectMemberRepository != null && !projectMemberRepository.findByUserId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete this user because project memberships still exist");
        }
        if (taskRepository != null && !taskRepository.findByAssigneeId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete this user because tasks are still assigned to them");
        }
        userRepository.deleteById(id);
    }

    private void applyProfile(User target, User request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User details are required");
        }
        String name = request.getName() == null ? "" : request.getName().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        userRepository.findByEmailIgnoreCase(email)
            .filter(existing -> target.getId() == null || !target.getId().equals(existing.getId()))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
            });
        target.setName(name);
        target.setEmail(email);
        target.setRole(request.getRole());
    }
}
