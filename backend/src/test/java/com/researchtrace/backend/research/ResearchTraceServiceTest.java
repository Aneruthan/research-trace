package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.DecisionLineageView;
import com.researchtrace.backend.dto.research.ExperimentRequest;
import com.researchtrace.backend.dto.research.ResearchDecisionRequest;
import com.researchtrace.backend.dto.research.ResearchTraceSummary;
import com.researchtrace.backend.dto.research.ResultRequest;
import com.researchtrace.backend.dto.research.SupersedeDecisionRequest;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResearchTraceServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long OTHER_PROJECT_ID = 99L;
    private static final Long MEETING_ID = 20L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private ResearchEventRepository researchEventRepository;

    @Mock
    private ResearchChangeRepository researchChangeRepository;

    private ResearchTraceService researchTraceService;

    @BeforeEach
    void setUp() {
        researchTraceService = new ResearchTraceService(
            projectRepository,
            meetingRepository,
            researchDecisionRepository,
            experimentRepository,
            resultRepository,
            researchEventRepository,
            researchChangeRepository
        );
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    }

    @Test
    void createDecisionSavesDecisionAndCreatesResearchEvent() {
        ResearchDecisionRequest request = new ResearchDecisionRequest();
        request.setTitle("Adopt PMC-VQA");
        request.setDecisionText("Use PMC-VQA for visual question answering.");
        request.setRationale("Industry standard benchmark.");
        request.setDecidedAt(LocalDate.of(2026, 10, 10));

        when(researchDecisionRepository.save(any(ResearchDecision.class)))
            .thenAnswer(inv -> {
                ResearchDecision d = inv.getArgument(0);
                d.setId(101L);
                return d;
            });

        ResearchDecision created = researchTraceService.createDecision(PROJECT_ID, request);

        assertThat(created.getId()).isEqualTo(101L);
        assertThat(created.getTitle()).isEqualTo("Adopt PMC-VQA");
        assertThat(created.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        ResearchEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getEventType()).isEqualTo(ResearchEventType.DECISION);
        assertThat(savedEvent.getResearchDecisionId()).isEqualTo(101L);
        assertThat(savedEvent.getTitle()).contains("Adopt PMC-VQA");
    }

    @Test
    void supersedeDecisionUpdatesOldDecisionAndLinksNewDecision() {
        ResearchDecision oldDecision = new ResearchDecision();
        oldDecision.setId(101L);
        oldDecision.setProjectId(PROJECT_ID);
        oldDecision.setTitle("Use PMC-VQA");
        oldDecision.setStatus(ResearchDecisionStatus.ACTIVE);

        when(researchDecisionRepository.findById(101L)).thenReturn(Optional.of(oldDecision));
        when(researchDecisionRepository.save(any(ResearchDecision.class)))
            .thenAnswer(inv -> {
                ResearchDecision d = inv.getArgument(0);
                if (d.getId() == null) d.setId(102L);
                return d;
            });

        SupersedeDecisionRequest request = new SupersedeDecisionRequest();
        request.setTitle("Switch to LitMedImage");
        request.setRationale("PMC-VQA had noisy labels in experiments.");
        request.setDecidedAt(LocalDate.of(2026, 10, 20));

        ResearchDecision newDecision = researchTraceService.supersedeDecision(PROJECT_ID, 101L, request);

        assertThat(oldDecision.getStatus()).isEqualTo(ResearchDecisionStatus.SUPERSEDED);
        assertThat(newDecision.getId()).isEqualTo(102L);
        assertThat(newDecision.getSupersedesDecisionId()).isEqualTo(101L);
        assertThat(newDecision.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);

        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(ResearchEventType.CHANGE);
        assertThat(eventCaptor.getValue().getDescription()).contains("PMC-VQA");
    }

    @Test
    void cannotSupersedeAlreadySupersededDecision() {
        ResearchDecision alreadySuperseded = new ResearchDecision();
        alreadySuperseded.setId(101L);
        alreadySuperseded.setProjectId(PROJECT_ID);
        alreadySuperseded.setStatus(ResearchDecisionStatus.SUPERSEDED);

        when(researchDecisionRepository.findById(101L)).thenReturn(Optional.of(alreadySuperseded));

        SupersedeDecisionRequest request = new SupersedeDecisionRequest();
        request.setTitle("Another decision");
        request.setRationale("Reason");

        assertThatThrownBy(() -> researchTraceService.supersedeDecision(PROJECT_ID, 101L, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("already superseded");
    }

    @Test
    void rejectsCrossProjectDecisionSupersession() {
        ResearchDecision otherProjectDecision = new ResearchDecision();
        otherProjectDecision.setId(101L);
        otherProjectDecision.setProjectId(OTHER_PROJECT_ID);

        when(researchDecisionRepository.findById(101L)).thenReturn(Optional.of(otherProjectDecision));

        SupersedeDecisionRequest request = new SupersedeDecisionRequest();
        request.setTitle("Switch");
        request.setRationale("Reason");

        assertThatThrownBy(() -> researchTraceService.supersedeDecision(PROJECT_ID, 101L, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("does not belong to project");
    }

    @Test
    void createExperimentValidatesDecisionBelongsToSameProject() {
        ResearchDecision otherDecision = new ResearchDecision();
        otherDecision.setId(50L);
        otherDecision.setProjectId(OTHER_PROJECT_ID);

        when(researchDecisionRepository.findById(50L)).thenReturn(Optional.of(otherDecision));

        ExperimentRequest request = new ExperimentRequest();
        request.setTitle("Test run");
        request.setResearchDecisionId(50L);

        assertThatThrownBy(() -> researchTraceService.createExperiment(PROJECT_ID, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("belongs to a different project");
    }

    @Test
    void recordResultValidatesExperimentBelongsToSameProject() {
        Experiment otherExp = new Experiment();
        otherExp.setId(70L);
        otherExp.setProjectId(OTHER_PROJECT_ID);

        when(experimentRepository.findById(70L)).thenReturn(Optional.of(otherExp));

        ResultRequest request = new ResultRequest();
        request.setExperimentId(70L);
        request.setSummary("Accuracy 60%");

        assertThatThrownBy(() -> researchTraceService.recordResult(PROJECT_ID, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("belongs to a different project");
    }

    @Test
    void retrievesFullDecisionEvidenceLineage() {
        ResearchDecision current = new ResearchDecision();
        current.setId(102L);
        current.setProjectId(PROJECT_ID);
        current.setTitle("LitMedImage approach");
        current.setSupersedesDecisionId(101L);
        current.setMeetingId(MEETING_ID);

        ResearchDecision previous = new ResearchDecision();
        previous.setId(101L);
        previous.setTitle("Old PMC-VQA approach");

        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setTitle("Review Meeting Oct 20");

        Experiment exp = new Experiment();
        exp.setId(500L);
        exp.setTitle("Evaluation on LitMedImage");

        Result res = new Result();
        res.setId(600L);
        res.setSummary("Achieved 74.2% accuracy");

        when(researchDecisionRepository.findById(102L)).thenReturn(Optional.of(current));
        when(researchDecisionRepository.findById(101L)).thenReturn(Optional.of(previous));
        when(researchDecisionRepository.findBySupersedesDecisionId(102L)).thenReturn(Optional.empty());
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(experimentRepository.findByResearchDecisionId(102L)).thenReturn(List.of(exp));
        when(resultRepository.findByExperimentId(500L)).thenReturn(List.of(res));

        DecisionLineageView lineage = researchTraceService.getDecisionLineage(PROJECT_ID, 102L);

        assertThat(lineage.getDecision().getTitle()).isEqualTo("LitMedImage approach");
        assertThat(lineage.getPredecessorDecision().getTitle()).isEqualTo("Old PMC-VQA approach");
        assertThat(lineage.getSourceMeeting().getTitle()).isEqualTo("Review Meeting Oct 20");
        assertThat(lineage.getExperiments()).hasSize(1);
        assertThat(lineage.getExperiments().get(0).getExperiment().getTitle()).isEqualTo("Evaluation on LitMedImage");
        assertThat(lineage.getExperiments().get(0).getResults()).containsExactly(res);
    }

    @Test
    void getTimelineReturnsChronologicalEvents() {
        ResearchEvent e1 = new ResearchEvent();
        e1.setTitle("Decision 1");
        e1.setOccurredAt(LocalDateTime.of(2026, 10, 10, 10, 0));

        ResearchEvent e2 = new ResearchEvent();
        e2.setTitle("Experiment 1");
        e2.setOccurredAt(LocalDateTime.of(2026, 10, 12, 14, 0));

        when(researchEventRepository.findByProjectIdOrderByOccurredAtAscCreatedAtAsc(PROJECT_ID))
            .thenReturn(List.of(e1, e2));

        List<ResearchEvent> timeline = researchTraceService.getTimeline(PROJECT_ID);
        assertThat(timeline).containsExactly(e1, e2);
    }

    @Test
    void getSummaryAggregatesCountsAccurately() {
        ResearchDecision d1 = new ResearchDecision();
        d1.setStatus(ResearchDecisionStatus.ACTIVE);
        ResearchDecision d2 = new ResearchDecision();
        d2.setStatus(ResearchDecisionStatus.SUPERSEDED);

        Experiment exp1 = new Experiment();
        exp1.setStatus(ExperimentStatus.COMPLETED);
        Experiment exp2 = new Experiment();
        exp2.setStatus(ExperimentStatus.RUNNING);

        when(researchDecisionRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID)).thenReturn(List.of(d1, d2));
        when(experimentRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID)).thenReturn(List.of(exp1, exp2));
        when(resultRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID)).thenReturn(Collections.emptyList());
        when(researchEventRepository.findByProjectIdOrderByOccurredAtAscCreatedAtAsc(PROJECT_ID)).thenReturn(Collections.emptyList());
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID)).thenReturn(Collections.emptyList());

        ResearchTraceSummary summary = researchTraceService.getSummary(PROJECT_ID);

        assertThat(summary.getTotalDecisions()).isEqualTo(2);
        assertThat(summary.getActiveDecisions()).isEqualTo(1);
        assertThat(summary.getTotalExperiments()).isEqualTo(2);
        assertThat(summary.getCompletedExperiments()).isEqualTo(1);
    }
}
