package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.service.ProgressService;
import com.researchtrace.backend.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    private final ProgressService progressService;

    public ProjectController(
            ProjectService projectService,
            ProgressService progressService) {

        this.projectService = projectService;
        this.progressService = progressService;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id)
            .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @GetMapping("/{projectId}/progress")
    public BigDecimal getProjectProgress(@PathVariable Long projectId) {
        return progressService.getProjectProgress(projectId);
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.saveProject(project);
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        return projectService.saveProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}