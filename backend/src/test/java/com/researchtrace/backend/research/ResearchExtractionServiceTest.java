package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResearchExtractionServiceTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private ResearchNotesExtractor researchNotesExtractor;

    @InjectMocks
    private ResearchExtractionService researchExtractionService;

    @Test
    void returnsExtractorOutputForTheMeetingsNotes() {
        Meeting meeting = meeting("The group decided to keep the current baseline.");
        MeetingExtractionResponse extracted = new MeetingExtractionResponse();
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        decision.setConfidence(0.8);
        extracted.setDecisions(List.of(decision));
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting));
        when(researchNotesExtractor.extract(meeting.getNotes())).thenReturn(extracted);

        MeetingExtractionResponse response = researchExtractionService.extract(21L);

        assertThat(response.getDecisions()).containsExactly(decision);
        assertThat(response.getTasks()).isNull();
        verify(researchNotesExtractor).extract(meeting.getNotes());
    }

    @Test
    void missingMeetingIsNotFound() {
        when(meetingService.getMeetingById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> researchExtractionService.extract(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
        verify(researchNotesExtractor, never()).extract(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankNotesAreRejected() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting("  ")));

        assertThatThrownBy(() -> researchExtractionService.extract(21L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting has no notes");
        verify(researchNotesExtractor, never()).extract(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unconfiguredExtractorDoesNotCallGemini() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting("Notes")));
        researchExtractionService = new ResearchExtractionService(
            meetingService,
            new UnconfiguredResearchNotesExtractor());

        assertThatThrownBy(() -> researchExtractionService.extract(21L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not configured");
    }

    private static Meeting meeting(String notes) {
        Meeting meeting = new Meeting();
        meeting.setId(21L);
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 7));
        meeting.setNotes(notes);
        return meeting;
    }
}
