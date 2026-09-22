package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.MeetingExtractionResponse;
import com.researchtrace.backend.service.MeetingExtractionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
public class MeetingExtractionController {

    private final MeetingExtractionService meetingExtractionService;

    public MeetingExtractionController(MeetingExtractionService meetingExtractionService) {
        this.meetingExtractionService = meetingExtractionService;
    }

    @PostMapping("/extract")
    public MeetingExtractionResponse extractMeetingNotes(
            @RequestBody String meetingNotes) {

        return meetingExtractionService.extractFromMeetingNotes(meetingNotes);
    }
}