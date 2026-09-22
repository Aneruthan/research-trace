package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.MeetingExtractionResponse;
import org.springframework.stereotype.Service;

@Service
public class MeetingExtractionService {

    public MeetingExtractionResponse extractFromMeetingNotes(String meetingNotes) {
        return new MeetingExtractionResponse();
    }
}