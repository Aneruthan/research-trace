package com.researchtrace.backend.service;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository) {
        this(projectMemberRepository, null);
    }

    @Autowired
    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            @Nullable UserRepository userRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public List<ProjectMember> getAllProjectMembers() {
        return projectMemberRepository.findAll();
    }

    public List<ProjectMember> getMembersByProject(Long projectId) {
        return projectMemberRepository.findByProjectId(projectId);
    }

    public Optional<ProjectMember> getProjectMemberById(Long id) {
        return projectMemberRepository.findById(id);
    }

    public ProjectMember saveProjectMember(ProjectMember projectMember) {
        return projectMemberRepository.save(projectMember);
    }

    public void deleteProjectMember(Long id) {
        projectMemberRepository.deleteById(id);
    }

    public ProjectMember getMemberForProject(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project member not found"));
        if (projectId == null || !projectId.equals(member.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project member not found");
        }
        return member;
    }

    public ProjectMember addMember(Long projectId, ProjectMember request) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ID is required");
        }
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required");
        }
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        if (userRepository != null && userRepository.findById(request.getUserId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }
        boolean alreadyMember = projectMemberRepository.findByProjectId(projectId).stream()
            .anyMatch(member -> request.getUserId().equals(member.getUserId()));
        if (alreadyMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this project");
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(request.getUserId());
        member.setRole(request.getRole());
        return projectMemberRepository.save(member);
    }

    public ProjectMember updateMember(Long projectId, Long memberId, ProjectMember request) {
        ProjectMember existing = getMemberForProject(projectId, memberId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Membership details are required");
        }
        if (request.getProjectId() != null && !projectId.equals(request.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Membership cannot be moved to another project");
        }
        if (request.getUserId() != null && !request.getUserId().equals(existing.getUserId())) {
            if (userRepository != null && userRepository.findById(request.getUserId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
            }
            existing.setUserId(request.getUserId());
        }
        if (request.getRole() != null) {
            existing.setRole(request.getRole());
        }
        return projectMemberRepository.save(existing);
    }

    public void removeMember(Long projectId, Long memberId) {
        getMemberForProject(projectId, memberId);
        projectMemberRepository.deleteById(memberId);
    }
}