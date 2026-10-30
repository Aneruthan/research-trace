package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
    ExtractionReviewService.class,
    MeetingService.class,
    AcceptedReviewRollbackTest.FailingRecorderConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AcceptedReviewRollbackTest {

    @Autowired
    private ExtractionReviewService extractionReviewService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ExtractionReviewRepository extractionReviewRepository;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    private Long meetingId;

    @BeforeEach
    void setUp() {
        researchDecisionRepository.deleteAll();
        extractionReviewRepository.deleteAll();
        meetingRepository.deleteAll();
        Meeting meeting = new Meeting();
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 7));
        meeting.setNotes("Notes");
        meetingId = meetingRepository.save(meeting).getId();
    }

    @Test
    void conversionFailureRollsBackTheReviewAndPartialRecords() {
        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(meetingId, request()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("conversion failed");

        assertThat(extractionReviewRepository.count()).isZero();
        assertThat(researchDecisionRepository.count()).isZero();
    }

    private ExtractionReviewRequest request() {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setDecisions(List.of(decision));
        ExtractionReviewRequest request = new ExtractionReviewRequest();
        request.setStatus(ExtractionReviewStatus.ACCEPTED);
        request.setExtraction(extraction);
        return request;
    }

    static class FailingRecorderConfig {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AcceptedReviewRecorder acceptedReviewRecorder(ResearchDecisionRepository researchDecisionRepository) {
            return new AcceptedReviewRecorder(null, researchDecisionRepository, null, null, null) {
                @Override
                public void record(ExtractionReview review, Long projectId, MeetingExtractionResponse extraction) {
                    ResearchDecision decision = new ResearchDecision();
                    decision.setProjectId(projectId);
                    decision.setTitle("partial");
                    decision.setStatus(ResearchDecisionStatus.ACTIVE);
                    decision.setExtractionReviewId(review.getId());
                    researchDecisionRepository.save(decision);
                    throw new IllegalStateException("conversion failed");
                }
            };
        }
    }
}
