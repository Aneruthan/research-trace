package com.researchtrace.backend.service;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
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
}