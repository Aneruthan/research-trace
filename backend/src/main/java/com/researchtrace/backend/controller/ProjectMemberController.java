package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.service.ProjectMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMember addMember(
            @PathVariable Long projectId,
            @RequestBody ProjectMember projectMember) {
        return projectMemberService.addMember(projectId, projectMember);
    }

    @GetMapping("/{memberId}")
    public ProjectMember getMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {
        return projectMemberService.getMemberForProject(projectId, memberId);
    }

    @PutMapping("/{memberId}")
    public ProjectMember updateMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody ProjectMember projectMember) {
        return projectMemberService.updateMember(projectId, memberId, projectMember);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {
        projectMemberService.removeMember(projectId, memberId);
    }
}
