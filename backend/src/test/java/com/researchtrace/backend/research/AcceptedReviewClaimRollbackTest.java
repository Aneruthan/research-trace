package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
    ExtractionReviewService.class,
    MeetingService.class,
    AcceptedReviewClaimRollbackTest.FailOnceConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AcceptedReviewClaimRollbackTest {

    @Autowired
    private ExtractionReviewService extractionReviewService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ExtractionReviewRepository extractionReviewRepository;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ResearchEventRepository researchEventRepository;

    @Autowired
    private ResearchReviewClaimRepository researchReviewClaimRepository;

    private Long meetingId;

    @BeforeEach
    void setUp() {
        FailOnceConfig.attempts.set(0);
        researchEventRepository.deleteAll();
        resultRepository.deleteAll();
        experimentRepository.deleteAll();
        researchDecisionRepository.deleteAll();
        researchReviewClaimRepository.deleteAll();
        extractionReviewRepository.deleteAll();
        meetingRepository.deleteAll();

        Meeting meeting = new Meeting();
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 7));
        meeting.setNotes("Keep the baseline.");
        meetingId = meetingRepository.save(meeting).getId();
    }

    @Test
    void aFailedAcceptRollsBackTheClaimSoTheRetryPersistsOnce() {
        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(meetingId, request()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("conversion failed");

        assertThat(extractionReviewRepository.count()).isZero();
        assertThat(researchReviewClaimRepository.count()).isZero();
        assertThat(researchDecisionRepository.count()).isZero();
        assertThat(experimentRepository.count()).isZero();
        assertThat(resultRepository.count()).isZero();
        assertThat(researchEventRepository.count()).isZero();

        extractionReviewService.reviewExtraction(meetingId, request());
        extractionReviewService.reviewExtraction(meetingId, request());

        assertThat(extractionReviewRepository.count()).isEqualTo(2);
        assertThat(researchReviewClaimRepository.count()).isEqualTo(1);
        assertThat(researchDecisionRepository.count()).isEqualTo(1);
        assertThat(researchEventRepository.count()).isEqualTo(1);
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

    static class FailOnceConfig {
        static final AtomicInteger attempts = new AtomicInteger();

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AcceptedReviewRecorder acceptedReviewRecorder(
                ResearchReviewClaimRepository researchReviewClaimRepository,
                ResearchDecisionRepository researchDecisionRepository,
                ExperimentRepository experimentRepository,
                ResultRepository resultRepository,
                ResearchEventRepository researchEventRepository) {
            return new AcceptedReviewRecorder(
                    researchReviewClaimRepository,
                    researchDecisionRepository,
                    experimentRepository,
                    resultRepository,
                    researchEventRepository) {
                @Override
                public void record(ExtractionReview review, Long projectId, MeetingExtractionResponse extraction) {
                    super.record(review, projectId, extraction);
                    if (attempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("conversion failed");
                    }
                }
            };
        }
    }
}
