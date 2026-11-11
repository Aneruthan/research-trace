package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public List<Meeting> getAllMeetings() {
        return meetingService.getAllMeetings();
    }

    @GetMapping("/{id}")
    public Meeting getMeetingById(@PathVariable Long id) {
        return meetingService.getMeetingById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Meeting createMeeting(@RequestBody Meeting meeting) {
        return meetingService.saveMeeting(meeting);
    }

    @PutMapping("/{id}")
    public Meeting updateMeeting(
        @PathVariable Long id,
        @RequestBody Meeting meeting) {

        return meetingService.updateMeeting(id, meeting);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
    }
}
