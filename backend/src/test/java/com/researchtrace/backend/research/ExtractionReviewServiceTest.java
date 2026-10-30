package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionReviewServiceTest {

    @Mock
    private ExtractionReviewRepository extractionReviewRepository;

    @Mock
    private MeetingService meetingService;

    @Mock
    private AcceptedReviewRecorder acceptedReviewRecorder;

    private ExtractionReviewService extractionReviewService;

    @BeforeEach
    void setUp() {
        extractionReviewService = new ExtractionReviewService(
            extractionReviewRepository,
            meetingService,
            JsonMapper.builder().build(),
            acceptedReviewRecorder);
        lenient().when(extractionReviewRepository.saveAndFlush(any(ExtractionReview.class)))
            .thenAnswer(invocation -> {
                ExtractionReview review = invocation.getArgument(0);
                review.setId(5L);
                return review;
            });
    }

    @Test
    void storesStructuredExtractionWithoutCreatingTasks() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting()));
        ExtractionReviewRequest request = request(ExtractionReviewStatus.EDITED);

        ExtractionReview saved = extractionReviewService.reviewExtraction(21L, request);

        ArgumentCaptor<ExtractionReview> captor = ArgumentCaptor.forClass(ExtractionReview.class);
        verify(extractionReviewRepository).saveAndFlush(captor.capture());
        assertThat(saved.getMeetingId()).isEqualTo(21L);
        assertThat(saved.getStatus()).isEqualTo(ExtractionReviewStatus.EDITED);
        assertThat(saved.getExtractionJson()).contains("Keep the baseline");
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(acceptedReviewRecorder, never()).record(any(), any(), any());
    }

    @Test
    void acceptedReviewRecordsUsingTheMeetingProject() {
        Meeting meeting = meeting();
        meeting.setProjectId(1L);
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting));

        ExtractionReview saved = extractionReviewService.reviewExtraction(21L, request(ExtractionReviewStatus.ACCEPTED));

        verify(acceptedReviewRecorder).record(eq(saved), eq(1L), any());
    }

    @Test
    void acceptedReviewRequiresTheMeetingProject() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting()));

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(21L, request(ExtractionReviewStatus.ACCEPTED)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not linked to a project");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
        verify(acceptedReviewRecorder, never()).record(any(), any(), any());
    }

    @Test
    void rejectsMissingStatusOrExtraction() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting()));
        ExtractionReviewRequest missingStatus = request(ExtractionReviewStatus.PENDING);
        missingStatus.setStatus(null);
        ExtractionReviewRequest missingExtraction = request(ExtractionReviewStatus.ACCEPTED);
        missingExtraction.setExtraction(null);

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(21L, missingStatus))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(21L, missingExtraction))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingMeetingIsNotFound() {
        when(meetingService.getMeetingById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(99L, request(ExtractionReviewStatus.REJECTED)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
        assertThatThrownBy(() -> extractionReviewService.getReviewsForMeeting(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsStoredReviewsForTheMeeting() {
        when(meetingService.getMeetingById(21L)).thenReturn(Optional.of(meeting()));
        ExtractionReview review = new ExtractionReview();
        review.setMeetingId(21L);
        review.setStatus(ExtractionReviewStatus.PENDING);
        when(extractionReviewRepository.findByMeetingIdOrderByCreatedAtAsc(21L)).thenReturn(List.of(review));

        assertThat(extractionReviewService.getReviewsForMeeting(21L)).containsExactly(review);
    }

    private static ExtractionReviewRequest request(ExtractionReviewStatus status) {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        decision.setConfidence(0.9);
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setDecisions(List.of(decision));
        ExtractionReviewRequest request = new ExtractionReviewRequest();
        request.setStatus(status);
        request.setExtraction(extraction);
        return request;
    }

    private static Meeting meeting() {
        Meeting meeting = new Meeting();
        meeting.setId(21L);
        meeting.setMeetingDate(LocalDate.of(2026, 10, 12));
        meeting.setNotes("Notes");
        return meeting;
    }
}
