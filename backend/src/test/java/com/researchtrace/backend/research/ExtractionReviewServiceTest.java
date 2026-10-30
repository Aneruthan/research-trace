package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionReviewServiceTest {

    private static final Long MEETING_ID = 21L;
    private static final Long PROJECT_ID = 10L;

    @Mock
    private ExtractionReviewRepository extractionReviewRepository;

    @Mock
    private MeetingService meetingService;

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private ResearchEventRepository researchEventRepository;

    @Mock
    private ChangeDetectionService changeDetectionService;

    private ExtractionReviewService extractionReviewService;

    @BeforeEach
    void setUp() {
        extractionReviewService = new ExtractionReviewService(
            extractionReviewRepository,
            meetingService,
            JsonMapper.builder().build(),
            researchDecisionRepository,
            experimentRepository,
            resultRepository,
            researchEventRepository,
            changeDetectionService
        );
        lenient().when(extractionReviewRepository.save(any(ExtractionReview.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(researchDecisionRepository.save(any(ResearchDecision.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(experimentRepository.save(any(Experiment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(resultRepository.save(any(Result.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(researchEventRepository.save(any(ResearchEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void acceptedExtractionPromotesReviewedItemsIntoDomainRecords() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));
        when(researchDecisionRepository.findByMeetingId(MEETING_ID)).thenReturn(Collections.emptyList());
        when(experimentRepository.findByMeetingId(MEETING_ID)).thenReturn(Collections.emptyList());
        when(resultRepository.findByMeetingId(MEETING_ID)).thenReturn(Collections.emptyList());

        when(researchDecisionRepository.save(any(ResearchDecision.class)))
            .thenAnswer(inv -> {
                ResearchDecision d = inv.getArgument(0);
                d.setId(101L);
                return d;
            });
        when(experimentRepository.save(any(Experiment.class)))
            .thenAnswer(inv -> {
                Experiment e = inv.getArgument(0);
                e.setId(201L);
                return e;
            });
        when(resultRepository.save(any(Result.class)))
            .thenAnswer(inv -> {
                Result r = inv.getArgument(0);
                r.setId(301L);
                return r;
            });

        ExtractionReviewRequest request = fullRequest(ExtractionReviewStatus.ACCEPTED);

        ExtractionReview saved = extractionReviewService.reviewExtraction(MEETING_ID, request);

        assertThat(saved.getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(saved.getStatus()).isEqualTo(ExtractionReviewStatus.ACCEPTED);

        // Verify decision created
        ArgumentCaptor<ResearchDecision> decisionCaptor = ArgumentCaptor.forClass(ResearchDecision.class);
        verify(researchDecisionRepository).save(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getTitle()).isEqualTo("Adopt PMC-VQA");
        assertThat(decisionCaptor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(decisionCaptor.getValue().getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(decisionCaptor.getValue().getSourceReference()).contains("Meeting 21 (Biomedical VQA Meeting)");

        // Verify experiment created
        ArgumentCaptor<Experiment> expCaptor = ArgumentCaptor.forClass(Experiment.class);
        verify(experimentRepository).save(expCaptor.capture());
        assertThat(expCaptor.getValue().getTitle()).isEqualTo("Baseline Med-CLIP Evaluation");

        // Verify result created
        ArgumentCaptor<Result> resCaptor = ArgumentCaptor.forClass(Result.class);
        verify(resultRepository).save(resCaptor.capture());
        assertThat(resCaptor.getValue().getSummary()).isEqualTo("Accuracy 58.4%");

        // Verify research events published
        verify(researchEventRepository, times(3)).save(any(ResearchEvent.class));

        // Verify change detection triggered
        verify(changeDetectionService).detectAndFlagChanges(eq(PROJECT_ID), eq(MEETING_ID), eq(request.getExtraction()));
    }

    @Test
    void editedExtractionIngestsReviewedValues() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));
        when(researchDecisionRepository.findByMeetingId(MEETING_ID)).thenReturn(Collections.emptyList());

        ExtractionReviewRequest request = fullRequest(ExtractionReviewStatus.EDITED);
        request.getExtraction().getDecisions().get(0).setTitle("Edited Title: Adopt PMC-VQA v2");

        when(researchDecisionRepository.save(any(ResearchDecision.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        extractionReviewService.reviewExtraction(MEETING_ID, request);

        ArgumentCaptor<ResearchDecision> decisionCaptor = ArgumentCaptor.forClass(ResearchDecision.class);
        verify(researchDecisionRepository).save(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getTitle()).isEqualTo("Edited Title: Adopt PMC-VQA v2");
    }

    @Test
    void rejectedExtractionNeverIngestsDomainRecords() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        ExtractionReviewRequest request = fullRequest(ExtractionReviewStatus.REJECTED);

        extractionReviewService.reviewExtraction(MEETING_ID, request);

        verify(researchDecisionRepository, never()).save(any());
        verify(experimentRepository, never()).save(any());
        verify(resultRepository, never()).save(any());
        verify(researchEventRepository, never()).save(any());
        verify(changeDetectionService, never()).detectAndFlagChanges(any(), any(), any());
    }

    @Test
    void repeatedReviewAcceptanceDoesNotCreateDuplicateRecords() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));

        ResearchDecision existingDecision = new ResearchDecision();
        existingDecision.setTitle("Adopt PMC-VQA");
        existingDecision.setMeetingId(MEETING_ID);
        when(researchDecisionRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(existingDecision));

        Experiment existingExp = new Experiment();
        existingExp.setTitle("Baseline Med-CLIP Evaluation");
        existingExp.setMeetingId(MEETING_ID);
        when(experimentRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(existingExp));

        Result existingRes = new Result();
        existingRes.setSummary("Accuracy 58.4%");
        existingRes.setMeetingId(MEETING_ID);
        when(resultRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(existingRes));

        ExtractionReviewRequest request = fullRequest(ExtractionReviewStatus.ACCEPTED);

        extractionReviewService.reviewExtraction(MEETING_ID, request);

        // Saved extraction review entity, but no new domain records created
        verify(researchDecisionRepository, never()).save(any());
        verify(experimentRepository, never()).save(any());
        verify(resultRepository, never()).save(any());
    }

    @Test
    void rejectsMissingStatusOrExtraction() {
        when(meetingService.getMeetingById(MEETING_ID)).thenReturn(Optional.of(meeting()));
        ExtractionReviewRequest missingStatus = new ExtractionReviewRequest();
        missingStatus.setExtraction(new MeetingExtractionResponse());

        ExtractionReviewRequest missingExtraction = new ExtractionReviewRequest();
        missingExtraction.setStatus(ExtractionReviewStatus.ACCEPTED);

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(MEETING_ID, missingStatus))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(MEETING_ID, missingExtraction))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
    }

    @Test
    void missingMeetingThrowsNotFound() {
        when(meetingService.getMeetingById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extractionReviewService.reviewExtraction(99L, fullRequest(ExtractionReviewStatus.ACCEPTED)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Meeting not found");
    }

    private static ExtractionReviewRequest fullRequest(ExtractionReviewStatus status) {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle("Adopt PMC-VQA");
        decision.setDescription("Primary benchmark");
        decision.setReason("Literature standard");

        ExtractedExperiment experiment = new ExtractedExperiment();
        experiment.setTitle("Baseline Med-CLIP Evaluation");
        experiment.setDescription("Run zero-shot evaluation");

        ExtractedResult result = new ExtractedResult();
        result.setDescription("Accuracy 58.4%");
        result.setExperiment("Baseline Med-CLIP Evaluation");
        result.setMetrics("58.4%");

        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setDecisions(List.of(decision));
        extraction.setExperiments(List.of(experiment));
        extraction.setResults(List.of(result));

        ExtractionReviewRequest request = new ExtractionReviewRequest();
        request.setStatus(status);
        request.setExtraction(extraction);
        return request;
    }

    private static Meeting meeting() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(PROJECT_ID);
        meeting.setTitle("Biomedical VQA Meeting");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 12));
        meeting.setNotes("Notes on PMC-VQA and baseline evaluation.");
        return meeting;
    }
}
