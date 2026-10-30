package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;

public interface ResearchNotesExtractor {

    MeetingExtractionResponse extract(String meetingNotes);
}
