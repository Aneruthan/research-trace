package com.researchtrace.backend.controller;

import com.researchtrace.backend.service.ProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/projects/{projectId}/students")
public class StudentController {

    private final ProgressService progressService;

    public StudentController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/{studentId}/progress")
    public BigDecimal getStudentProgress(
            @PathVariable Long projectId,
            @PathVariable Long studentId) {
        return progressService.getStudentProgress(studentId, projectId);
    }
}
