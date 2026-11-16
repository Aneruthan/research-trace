package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptedReviewRecorderDuplicateTest {

    @Mock
    private ResearchReviewClaimRepository researchReviewClaimRepository;
    @Mock
    private ResearchDecisionRepository researchDecisionRepository;
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private ResultRepository resultRepository;
    @Mock
    private ResearchEventRepository researchEventRepository;

    @Test
    void aSecondAcceptanceOfTheSameContentDoesNotCreateAnotherRecord() {
        AcceptedReviewRecorder recorder = new AcceptedReviewRecorder(
            researchReviewClaimRepository,
            researchDecisionRepository,
            experimentRepository,
            resultRepository,
            researchEventRepository);
        ExtractionReview review = new ExtractionReview();
        review.setId(5L);
        review.setMeetingId(21L);
        review.setExtractionJson("{\"decisions\":[{\"title\":\"Keep the baseline\"}]}");
        when(researchReviewClaimRepository.existsByMeetingIdAndContentHash(
            org.mockito.ArgumentMatchers.eq(21L),
            org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        recorder.record(review, 1L, new MeetingExtractionResponse());
        verify(researchDecisionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(researchReviewClaimRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}
