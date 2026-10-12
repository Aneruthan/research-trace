package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.service.ProjectMemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    public List<ProjectMember> getAllProjectMembers() {
        return projectMemberService.getAllProjectMembers();
    }

    @GetMapping("/{id}")
    public ProjectMember getProjectMemberById(@PathVariable Long id) {
        return projectMemberService.getProjectMemberById(id)
            .orElseThrow(() -> new RuntimeException("Project member not found"));
    }

    @PostMapping
    public ProjectMember createProjectMember(@RequestBody ProjectMember projectMember) {
        return projectMemberService.saveProjectMember(projectMember);
    }

    @PutMapping("/{id}")
    public ProjectMember updateProjectMember(
        @PathVariable Long id,
        @RequestBody ProjectMember projectMember) {

    projectMember.setId(id);
    return projectMemberService.saveProjectMember(projectMember);
}

    @DeleteMapping("/{id}")
    public void deleteProjectMember(@PathVariable Long id) {
        projectMemberService.deleteProjectMember(id);
    }
}