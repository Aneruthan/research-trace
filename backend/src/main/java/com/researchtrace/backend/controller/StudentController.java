package com.researchtrace.backend.controller;

import com.researchtrace.backend.service.ProgressService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final ProgressService progressService;

    public StudentController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/{studentId}/progress")
    public BigDecimal getStudentProgress(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long projectId) {
        return projectId != null
            ? progressService.getStudentProgress(studentId, projectId)
            : progressService.getStudentProgress(studentId);
    }
}
