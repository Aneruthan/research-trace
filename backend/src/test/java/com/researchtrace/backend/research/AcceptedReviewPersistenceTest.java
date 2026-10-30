package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
    ExtractionReviewService.class,
    AcceptedReviewRecorder.class,
    MeetingService.class,
    AcceptedReviewPersistenceTest.JsonMapperConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AcceptedReviewPersistenceTest {

    private static final Long PROJECT_ID = 1L;

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

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSuggestionRepository taskSuggestionRepository;

    private Long meetingId;

    @BeforeEach
    void setUp() {
        taskSuggestionRepository.deleteAll();
        taskRepository.deleteAll();
        researchEventRepository.deleteAll();
        resultRepository.deleteAll();
        experimentRepository.deleteAll();
        researchDecisionRepository.deleteAll();
        researchReviewClaimRepository.deleteAll();
        extractionReviewRepository.deleteAll();
        meetingRepository.deleteAll();

        Meeting meeting = new Meeting();
        meeting.setProjectId(PROJECT_ID);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 7));
        meeting.setNotes("The group decided to keep the baseline.");
        meetingId = meetingRepository.save(meeting).getId();
    }

    @Test
    void acceptingAReviewPersistsRecordsAndEvents() {
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.ACCEPTED, validExtraction()));

        assertThat(researchDecisionRepository.findByProjectIdAndStatus(PROJECT_ID, ResearchDecisionStatus.ACTIVE))
            .singleElement()
            .satisfies(decision -> {
                assertThat(decision.getTitle()).isEqualTo("Keep the baseline");
                assertThat(decision.getDecisionText()).isEqualTo("Continue measuring the current model.");
                assertThat(decision.getRationale()).isEqualTo("The bias numbers are not ready.");
                assertThat(decision.getMeetingId()).isEqualTo(meetingId);
                assertThat(decision.getExtractionReviewId()).isNotNull();
                assertThat(decision.getProjectId()).isEqualTo(PROJECT_ID);
                assertThat(decision.getSourceReference()).startsWith("meeting:" + meetingId + ";review:");
            });
        assertThat(experimentRepository.findByProjectId(PROJECT_ID))
            .singleElement()
            .satisfies(experiment -> {
                assertThat(experiment.getTitle()).isEqualTo("Bias audit");
                assertThat(experiment.getHypothesis()).isEqualTo("The baseline has a measurable gap.");
                assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
                assertThat(experiment.getResearchDecisionId()).isNull();
            });
        assertThat(resultRepository.findByProjectId(PROJECT_ID))
            .singleElement()
            .satisfies(result -> {
                assertThat(result.getSummary()).isEqualTo("Gender gap is 4 points.");
                assertThat(result.getDataOrMetric()).isEqualTo("accuracy gap = 0.04");
                assertThat(result.getExperimentId()).isNotNull();
            });
        assertThat(researchEventRepository.findByProjectIdOrderByOccurredAtAscIdAsc(PROJECT_ID))
            .extracting(event -> event.getEventType())
            .containsExactly(ResearchEventType.EXPERIMENT, ResearchEventType.DECISION, ResearchEventType.RESULT);
        assertThat(taskRepository.count()).isZero();
        assertThat(taskSuggestionRepository.count()).isZero();
    }

    @Test
    void pendingRejectedAndEditedReviewsCreateNoRecords() {
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.PENDING, validExtraction()));
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.REJECTED, validExtraction()));
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.EDITED, validExtraction()));

        assertThat(researchDecisionRepository.count()).isZero();
        assertThat(experimentRepository.count()).isZero();
        assertThat(resultRepository.count()).isZero();
        assertThat(researchEventRepository.count()).isZero();
        assertThat(extractionReviewRepository.count()).isEqualTo(3);
    }

    @Test
    void editedContentCreatesRecordsOnlyWhenAccepted() {
        MeetingExtractionResponse edited = validExtraction();
        edited.getDecisions().get(0).setTitle("Use a balanced split");
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.EDITED, edited));
        assertThat(researchDecisionRepository.count()).isZero();

        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.ACCEPTED, edited));
        assertThat(researchDecisionRepository.findByProjectIdOrderByDecidedAtAsc(PROJECT_ID))
            .extracting(decision -> decision.getTitle())
            .containsExactly("Use a balanced split");
    }

    @Test
    void acceptingTheSameContentTwiceDoesNotDuplicateRecords() {
        ExtractionReviewRequest review = request(ExtractionReviewStatus.ACCEPTED, validExtraction());
        extractionReviewService.reviewExtraction(meetingId, review);
        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.ACCEPTED, validExtraction()));

        assertThat(extractionReviewRepository.count()).isEqualTo(2);
        assertThat(researchDecisionRepository.count()).isEqualTo(1);
        assertThat(experimentRepository.count()).isEqualTo(1);
        assertThat(resultRepository.count()).isEqualTo(1);
        assertThat(researchEventRepository.count()).isEqualTo(3);
    }

    @Test
    void incompleteExtractionDoesNotFabricateRecords() {
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        ExtractedDecision untitled = new ExtractedDecision();
        untitled.setDescription("No title, so this is not a decision.");
        ExtractedExperiment untitledExperiment = new ExtractedExperiment();
        untitledExperiment.setObjective("No title.");
        ExtractedResult orphan = new ExtractedResult();
        orphan.setDescription("A result with no matching experiment.");
        orphan.setExperiment("Missing experiment");
        orphan.setMetrics("0.2");
        extraction.setDecisions(java.util.List.of(untitled));
        extraction.setExperiments(java.util.List.of(untitledExperiment));
        extraction.setResults(java.util.List.of(orphan));

        extractionReviewService.reviewExtraction(meetingId, request(ExtractionReviewStatus.ACCEPTED, extraction));

        assertThat(researchDecisionRepository.count()).isZero();
        assertThat(experimentRepository.count()).isZero();
        assertThat(resultRepository.count()).isZero();
        assertThat(researchEventRepository.count()).isZero();
        assertThat(extractionReviewRepository.count()).isEqualTo(1);
    }

    private ExtractionReviewRequest request(ExtractionReviewStatus status, MeetingExtractionResponse extraction) {
        ExtractionReviewRequest request = new ExtractionReviewRequest();
        request.setStatus(status);
        request.setExtraction(extraction);
        return request;
    }

    private MeetingExtractionResponse validExtraction() {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Keep the baseline");
        decision.setDescription("Continue measuring the current model.");
        decision.setReason("The bias numbers are not ready.");
        ExtractedExperiment experiment = new ExtractedExperiment();
        experiment.setTitle("Bias audit");
        experiment.setObjective("The baseline has a measurable gap.");
        experiment.setVariables("gender, skin tone");
        ExtractedResult result = new ExtractedResult();
        result.setDescription("Gender gap is 4 points.");
        result.setExperiment("Bias audit");
        result.setMetrics("accuracy gap = 0.04");
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setDecisions(java.util.List.of(decision));
        extraction.setExperiments(java.util.List.of(experiment));
        extraction.setResults(java.util.List.of(result));
        return extraction;
    }

    static class JsonMapperConfig {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }
}
