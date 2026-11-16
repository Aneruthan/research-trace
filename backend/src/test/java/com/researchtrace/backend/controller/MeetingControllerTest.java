package com.researchtrace.backend.controller;

import com.researchtrace.backend.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeetingService meetingService;

    @Test
    void createRejectsATitleLongerThan255() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 255 characters"))
            .when(meetingService).saveMeeting(any());

        mockMvc.perform(post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "m".repeat(256) + "\",\"meetingDate\":\"2026-11-08\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Title must be at most 255 characters"));
    }

    @Test
    void updateRejectsATitleLongerThan255BeforeReplacingTheMeeting() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 255 characters"))
            .when(meetingService).updateMeeting(org.mockito.ArgumentMatchers.eq(4L), any());

        mockMvc.perform(put("/api/meetings/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "m".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Title must be at most 255 characters"));

        verify(meetingService, never()).saveMeeting(any());
    }
}
