package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResearchExtractionService {

    private final MeetingService meetingService;
    private final ResearchNotesExtractor researchNotesExtractor;

    public ResearchExtractionService(
            MeetingService meetingService,
            ResearchNotesExtractor researchNotesExtractor) {
        this.meetingService = meetingService;
        this.researchNotesExtractor = researchNotesExtractor;
    }

    public MeetingExtractionResponse extract(Long projectId, Long meetingId) {
        meetingService.getMeetingForProject(projectId, meetingId);
        return extract(meetingId);
    }

    public MeetingExtractionResponse extract(Long meetingId) {
        Meeting meeting = meetingService.getMeetingById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
        if (meeting.getNotes() == null || meeting.getNotes().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting has no notes");
        }
        return researchNotesExtractor.extract(meeting.getNotes());
    }
}
