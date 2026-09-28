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
    public BigDecimal getStudentProgress(@PathVariable Long studentId) {
        return progressService.getStudentProgress(studentId);
    }
}
