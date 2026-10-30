package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResearchDomainRepositoryTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ResearchEventRepository researchEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesDecisionWithOptionalLinksNull() {
        ResearchDecision decision = decision(PROJECT_ID, "Keep the baseline", ResearchDecisionStatus.ACTIVE);
        decision.setDecisionText("Continue with the current vision-language baseline.");
        decision.setRationale("Bias numbers are not ready.");
        decision.setDecidedAt(LocalDateTime.of(2026, 10, 7, 15, 0));
        decision.setSourceReference("meeting:21");

        Long id = researchDecisionRepository.save(decision).getId();
        flushAndClear();

        ResearchDecision reloaded = researchDecisionRepository.findById(id).orElseThrow();
        assertThat(reloaded.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(reloaded.getMeetingId()).isNull();
        assertThat(reloaded.getExtractionReviewId()).isNull();
        assertThat(reloaded.getSupersedesDecisionId()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(reloaded.getSourceReference()).isEqualTo("meeting:21");
    }

    @Test
    void findsDecisionsForOneProjectByStatus() {
        researchDecisionRepository.save(decision(PROJECT_ID, "Keep the baseline", ResearchDecisionStatus.ACTIVE));
        researchDecisionRepository.save(decision(PROJECT_ID, "Old metric", ResearchDecisionStatus.SUPERSEDED));
        researchDecisionRepository.save(decision(OTHER_PROJECT_ID, "Other project", ResearchDecisionStatus.ACTIVE));
        flushAndClear();

        assertThat(researchDecisionRepository.findByProjectIdAndStatus(PROJECT_ID, ResearchDecisionStatus.ACTIVE))
            .extracting(ResearchDecision::getTitle)
            .containsExactly("Keep the baseline");
    }

    @Test
    void savesExperimentAndFindsItByDecision() {
        Long decisionId = researchDecisionRepository
            .save(decision(PROJECT_ID, "Keep the baseline", ResearchDecisionStatus.ACTIVE))
            .getId();
        Experiment experiment = experiment(PROJECT_ID, "Bias audit", ExperimentStatus.PLANNED);
        experiment.setResearchDecisionId(decisionId);
        experiment.setHypothesis("The baseline has a measurable gender gap.");
        experiment.setMethod("Evaluate on the balanced split.");
        experiment.setSourceReference("review:5");
        experimentRepository.save(experiment);
        flushAndClear();

        assertThat(experimentRepository.findByResearchDecisionId(decisionId))
            .singleElement()
            .satisfies(saved -> {
                assertThat(saved.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
                assertThat(saved.getStartedAt()).isNull();
                assertThat(saved.getCompletedAt()).isNull();
            });
        assertThat(experimentRepository.findByProjectId(OTHER_PROJECT_ID)).isEmpty();
    }

    @Test
    void savesResultForOneExperiment() {
        Long experimentId = experimentRepository
            .save(experiment(PROJECT_ID, "Bias audit", ExperimentStatus.RUNNING))
            .getId();
        Result result = new Result();
        result.setProjectId(PROJECT_ID);
        result.setExperimentId(experimentId);
        result.setSummary("Gender gap is 4 points.");
        result.setDataOrMetric("accuracy gap = 0.04");
        result.setObservedAt(LocalDateTime.of(2026, 10, 9, 18, 30));
        result.setSourceReference("meeting:21");
        resultRepository.save(result);
        flushAndClear();

        assertThat(resultRepository.findByExperimentId(experimentId))
            .extracting(Result::getSummary)
            .containsExactly("Gender gap is 4 points.");
        assertThat(resultRepository.findByProjectId(OTHER_PROJECT_ID)).isEmpty();
    }

    @Test
    void listsResearchEventsForOneProjectInTimeOrder() {
        researchEventRepository.save(event(PROJECT_ID, ResearchEventType.DECISION,
            LocalDateTime.of(2026, 10, 7, 16, 0), "Decision recorded"));
        researchEventRepository.save(event(PROJECT_ID, ResearchEventType.MEETING,
            LocalDateTime.of(2026, 10, 7, 15, 0), "Meeting held"));
        researchEventRepository.save(event(OTHER_PROJECT_ID, ResearchEventType.RESULT,
            LocalDateTime.of(2026, 10, 7, 14, 0), "Other project"));
        flushAndClear();

        assertThat(researchEventRepository.findByProjectIdOrderByOccurredAtAsc(PROJECT_ID))
            .extracting(ResearchEvent::getTitle)
            .containsExactly("Meeting held", "Decision recorded");
        assertThat(researchEventRepository.findByProjectIdAndEventType(PROJECT_ID, ResearchEventType.DECISION))
            .hasSize(1);
    }

    @Test
    void ordersEventsWithTheSameTimestampById() {
        LocalDateTime sameTime = LocalDateTime.of(2026, 10, 7, 15, 0);
        researchEventRepository.save(event(PROJECT_ID, ResearchEventType.MEETING, sameTime, "First"));
        researchEventRepository.save(event(PROJECT_ID, ResearchEventType.DECISION, sameTime, "Second"));
        flushAndClear();

        assertThat(researchEventRepository.findByProjectIdOrderByOccurredAtAscIdAsc(PROJECT_ID))
            .extracting(ResearchEvent::getTitle)
            .containsExactly("First", "Second");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResearchDecision decision(Long projectId, String title, ResearchDecisionStatus status) {
        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(projectId);
        decision.setTitle(title);
        decision.setStatus(status);
        return decision;
    }

    private Experiment experiment(Long projectId, String title, ExperimentStatus status) {
        Experiment experiment = new Experiment();
        experiment.setProjectId(projectId);
        experiment.setTitle(title);
        experiment.setStatus(status);
        return experiment;
    }

    private ResearchEvent event(Long projectId, ResearchEventType type, LocalDateTime occurredAt, String title) {
        ResearchEvent event = new ResearchEvent();
        event.setProjectId(projectId);
        event.setEventType(type);
        event.setOccurredAt(occurredAt);
        event.setTitle(title);
        return event;
    }
}
