package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.StudentWorkView;
import com.researchtrace.backend.service.StudentWorkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/students")
public class StudentWorkController {

    private final StudentWorkService studentWorkService;

    public StudentWorkController(StudentWorkService studentWorkService) {
        this.studentWorkService = studentWorkService;
    }

    @GetMapping
    public List<StudentWorkView> listStudents(@PathVariable Long projectId) {
        return studentWorkService.listStudents(projectId);
    }

    @GetMapping("/{studentId}")
    public StudentWorkView getStudent(
            @PathVariable Long projectId,
            @PathVariable Long studentId) {
        return studentWorkService.getStudent(projectId, studentId);
    }
}
