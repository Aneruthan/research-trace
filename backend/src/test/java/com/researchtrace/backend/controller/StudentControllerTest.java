package com.researchtrace.backend.controller;

import com.researchtrace.backend.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    @Test
    void returnsStudentProgress() throws Exception {
        when(progressService.getStudentProgress(7L)).thenReturn(new BigDecimal("50.00"));

        mockMvc.perform(get("/api/students/7/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("50.00"));
    }

    @Test
    void returnsZeroProgressForStudentWithoutTasks() throws Exception {
        when(progressService.getStudentProgress(9L)).thenReturn(new BigDecimal("0.00"));

        mockMvc.perform(get("/api/students/9/progress"))
            .andExpect(status().isOk())
            .andExpect(content().string("0.00"));
    }
}
