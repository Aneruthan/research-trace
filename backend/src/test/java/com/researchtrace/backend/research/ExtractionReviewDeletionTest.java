package com.researchtrace.backend.research;

import tools.jackson.databind.json.JsonMapper;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
import com.researchtrace.backend.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionReviewDeletionTest {

    @Mock private ExtractionReviewRepository extractionReviewRepository;
    @Mock private MeetingService meetingService;
    @Mock private AcceptedReviewRecorder acceptedReviewRecorder;
    @Mock private ResearchDecisionRepository researchDecisionRepository;
    @Mock private ExperimentRepository experimentRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private ResearchEventRepository researchEventRepository;
    @Mock private ResearchReviewClaimRepository researchReviewClaimRepository;

    private ExtractionReviewService service;

    @BeforeEach
    void setUp() {
        service = new ExtractionReviewService(
            extractionReviewRepository,
            meetingService,
            JsonMapper.builder().build(),
            acceptedReviewRecorder,
            null,
            researchDecisionRepository,
            experimentRepository,
            resultRepository,
            researchEventRepository,
            researchReviewClaimRepository);
        Meeting meeting = new Meeting();
        meeting.setId(7L);
        meeting.setProjectId(1L);
        when(meetingService.getMeetingForProject(1L, 7L)).thenReturn(meeting);
    }

    @Test
    void deletesAPendingReviewAndLeavesAcceptedResearchRecordsUntouched() {
        ExtractionReview review = review(4L, ExtractionReviewStatus.PENDING);
        when(extractionReviewRepository.findById(4L)).thenReturn(Optional.of(review));
        when(researchDecisionRepository.findByExtractionReviewId(4L)).thenReturn(List.of());
        when(experimentRepository.findByExtractionReviewId(4L)).thenReturn(List.of());
        when(resultRepository.findByExtractionReviewId(4L)).thenReturn(List.of());
        when(researchEventRepository.findByMeetingId(7L)).thenReturn(List.of());

        service.deleteReview(1L, 7L, 4L);

        verify(researchReviewClaimRepository).deleteByExtractionReviewId(4L);
        verify(extractionReviewRepository).delete(review);
    }

    @Test
    void refusesToDeleteAnAcceptedReviewThatStillHasResearchRecords() {
        ExtractionReview review = review(5L, ExtractionReviewStatus.ACCEPTED);
        when(extractionReviewRepository.findById(5L)).thenReturn(Optional.of(review));
        ResearchDecision decision = new ResearchDecision();
        decision.setId(9L);
        when(researchDecisionRepository.findByExtractionReviewId(5L)).thenReturn(List.of(decision));
        when(experimentRepository.findByExtractionReviewId(5L)).thenReturn(List.of());
        when(resultRepository.findByExtractionReviewId(5L)).thenReturn(List.of());
        when(researchEventRepository.findByMeetingId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteReview(1L, 7L, 5L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException status = (ResponseStatusException) error;
                assertThat(status.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(status.getReason()).contains("Decisions: 1");
            });
        verify(extractionReviewRepository, never()).delete(review);
    }

    @Test
    void aMissingReviewIsNotFoundAndASecondDeleteStaysNotFound() {
        when(extractionReviewRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteReview(1L, 7L, 99L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static ExtractionReview review(Long id, ExtractionReviewStatus status) {
        ExtractionReview review = new ExtractionReview();
        review.setId(id);
        review.setMeetingId(7L);
        review.setStatus(status);
        return review;
    }
}
