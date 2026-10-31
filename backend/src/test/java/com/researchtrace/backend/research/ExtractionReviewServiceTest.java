package com.researchtrace.backend.research;

import tools.jackson.databind.json.JsonMapper;
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

    private static final Long MEETING_ID = 21L;
    private static final Long PROJECT_ID = 1L;

    @Mock
    private ExtractionReviewRepository extractionReviewRepository;

    @Mock
    private MeetingService meetingService;

    @Mock
    private AcceptedReviewRecorder acceptedReviewRecorder;

    @Mock
    private ChangeDetectionService changeDetectionService;

    private ExtractionReviewService extractionReviewService;

    @BeforeEach
    void setUp() {
        extractionReviewService = new ExtractionReviewService(
            extractionReviewRepository,
            meetingService,
            JsonMapper.builder().build(),
            acceptedReviewRecorder,
            changeDetectionService);

        lenient().when(extractionReviewRepository.saveAndFlush(any(ExtractionReview.class)))
            .thenAnswer(invocation -> {
                ExtractionReview review = invocation.getArgument(0);
                review.setId(5L);
                return review;
            });
    }

    @Test
    void savesReview() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        ExtractionReview saved = extractionReviewService.reviewExtraction(MEETING_ID, request(ExtractionReviewStatus.EDITED));

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
        meeting.setProjectId(PROJECT_ID);
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting));

        ExtractionReview saved = extractionReviewService.reviewExtraction(MEETING_ID, request(ExtractionReviewStatus.ACCEPTED));

        verify(acceptedReviewRecorder).record(eq(saved), eq(PROJECT_ID), any());
        verify(changeDetectionService).detectAndFlagChanges(eq(PROJECT_ID), eq(MEETING_ID), any());
    }

    @Test
    void acceptedReviewRequiresTheMeetingProject() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(MEETING_ID, request(ExtractionReviewStatus.ACCEPTED)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not linked to a project");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
        verify(acceptedReviewRecorder, never()).record(any(), any(), any());
    }

    @Test
    void missingStatusRejected() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        ExtractionReviewRequest missingStatus = new ExtractionReviewRequest();
        missingStatus.setExtraction(new MeetingExtractionResponse());

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(MEETING_ID, missingStatus))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingExtractionRejected() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        ExtractionReviewRequest missingExtraction = new ExtractionReviewRequest();
        missingExtraction.setStatus(ExtractionReviewStatus.ACCEPTED);

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(MEETING_ID, missingExtraction))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingMeetingThrowsNotFound() {
        when(meetingService.getMeetingById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(99L, request(ExtractionReviewStatus.ACCEPTED)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
        assertThatThrownBy(() -> extractionReviewService.getReviewsForMeeting(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
        verify(extractionReviewRepository, never()).saveAndFlush(any());
    }

    private static ExtractionReviewRequest request(ExtractionReviewStatus status) {
        ExtractionReviewRequest request = new ExtractionReviewRequest();
        request.setStatus(status);
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        response.setDecisions(List.of(decision));
        request.setExtraction(response);
        return request;
    }

    private static Meeting meeting() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 7));
        meeting.setNotes("Notes");
        return meeting;
    }
}
