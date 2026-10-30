package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.DecisionSupersession;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ResearchDecisionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ResearchDecisionSupersessionTest {

    private static final Long PROJECT_ID = 1L;

    @Autowired
    private ResearchDecisionService researchDecisionService;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    @BeforeEach
    void setUp() {
        researchDecisionRepository.deleteAll();
    }

    @Test
    void supersessionPreservesBothDecisions() {
        ResearchDecision previous = save("Keep the baseline", PROJECT_ID, "Continue measuring.", "Bias numbers are not ready.");
        ResearchDecision replacement = save("Use a balanced split", PROJECT_ID, "Replace the old split.", "The sample was skewed.");

        DecisionSupersession result = researchDecisionService.supersede(PROJECT_ID, previous.getId(), replacement.getId());

        assertThat(researchDecisionRepository.count()).isEqualTo(2);
        ResearchDecision storedPrevious = researchDecisionRepository.findById(previous.getId()).orElseThrow();
        ResearchDecision storedReplacement = researchDecisionRepository.findById(replacement.getId()).orElseThrow();
        assertThat(storedPrevious.getStatus()).isEqualTo(ResearchDecisionStatus.SUPERSEDED);
        assertThat(storedPrevious.getTitle()).isEqualTo("Keep the baseline");
        assertThat(storedPrevious.getDecisionText()).isEqualTo("Continue measuring.");
        assertThat(storedPrevious.getRationale()).isEqualTo("Bias numbers are not ready.");
        assertThat(storedPrevious.getMeetingId()).isEqualTo(21L);
        assertThat(storedPrevious.getSourceReference()).isEqualTo("meeting:21;review:5");
        assertThat(storedPrevious.getSupersedesDecisionId()).isNull();
        assertThat(storedReplacement.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(storedReplacement.getTitle()).isEqualTo("Use a balanced split");
        assertThat(storedReplacement.getDecisionText()).isEqualTo("Replace the old split.");
        assertThat(storedReplacement.getSupersedesDecisionId()).isEqualTo(previous.getId());
        assertThat(result.superseded().getId()).isEqualTo(previous.getId());
        assertThat(result.replacement().getId()).isEqualTo(replacement.getId());
    }

    @Test
    void crossProjectSupersessionIsRejected() {
        ResearchDecision previous = save("Keep the baseline", PROJECT_ID, "Text", "Reason");
        ResearchDecision otherProject = save("Other project decision", 2L, "Text", "Reason");

        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, previous.getId(), otherProject.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("same project");
        assertThat(researchDecisionRepository.findById(previous.getId()).orElseThrow().getStatus())
            .isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(researchDecisionRepository.findById(otherProject.getId()).orElseThrow().getSupersedesDecisionId())
            .isNull();
    }

    @Test
    void selfSupersessionIsRejected() {
        ResearchDecision decision = save("Keep the baseline", PROJECT_ID, "Text", "Reason");

        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, decision.getId(), decision.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("cannot supersede itself");
        assertThat(researchDecisionRepository.findById(decision.getId()).orElseThrow().getStatus())
            .isEqualTo(ResearchDecisionStatus.ACTIVE);
    }

    @Test
    void cyclicSupersessionIsRejected() {
        ResearchDecision first = save("First", PROJECT_ID, "Text", "Reason");
        ResearchDecision second = save("Second", PROJECT_ID, "Text", "Reason");
        ResearchDecision third = save("Third", PROJECT_ID, "Text", "Reason");
        researchDecisionService.supersede(PROJECT_ID, first.getId(), second.getId());
        researchDecisionService.supersede(PROJECT_ID, second.getId(), third.getId());

        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, third.getId(), first.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("cycle");
        assertThat(researchDecisionRepository.findById(third.getId()).orElseThrow().getStatus())
            .isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(researchDecisionRepository.findById(first.getId()).orElseThrow().getSupersedesDecisionId())
            .isNull();
    }

    @Test
    void alreadySupersededDecisionIsRejected() {
        ResearchDecision first = save("First", PROJECT_ID, "Text", "Reason");
        ResearchDecision second = save("Second", PROJECT_ID, "Text", "Reason");
        ResearchDecision third = save("Third", PROJECT_ID, "Text", "Reason");
        researchDecisionService.supersede(PROJECT_ID, first.getId(), second.getId());

        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, first.getId(), third.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("already superseded");
        assertThat(researchDecisionRepository.findById(second.getId()).orElseThrow().getSupersedesDecisionId())
            .isEqualTo(first.getId());
        assertThat(researchDecisionRepository.findById(third.getId()).orElseThrow().getSupersedesDecisionId())
            .isNull();
    }

    @Test
    void missingDecisionIdsAreRejected() {
        ResearchDecision decision = save("Keep the baseline", PROJECT_ID, "Text", "Reason");

        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, decision.getId(), null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("required");
        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, 999L, decision.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not found");
        assertThatThrownBy(() -> researchDecisionService.supersede(PROJECT_ID, decision.getId(), 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not found");
        assertThat(researchDecisionRepository.count()).isEqualTo(1);
    }

    @Test
    void repeatingSupersessionDoesNotCreateAnotherDecision() {
        ResearchDecision previous = save("Keep the baseline", PROJECT_ID, "Text", "Reason");
        ResearchDecision replacement = save("Use a balanced split", PROJECT_ID, "Text", "Reason");
        researchDecisionService.supersede(PROJECT_ID, previous.getId(), replacement.getId());

        DecisionSupersession repeated = researchDecisionService.supersede(PROJECT_ID, previous.getId(), replacement.getId());

        assertThat(researchDecisionRepository.count()).isEqualTo(2);
        assertThat(repeated.superseded().getStatus()).isEqualTo(ResearchDecisionStatus.SUPERSEDED);
        assertThat(repeated.replacement().getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(repeated.replacement().getSupersedesDecisionId()).isEqualTo(previous.getId());
    }

    private ResearchDecision save(String title, Long projectId, String text, String rationale) {
        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(projectId);
        decision.setTitle(title);
        decision.setDecisionText(text);
        decision.setRationale(rationale);
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        decision.setMeetingId(21L);
        decision.setExtractionReviewId(5L);
        decision.setDecidedAt(LocalDateTime.of(2026, 10, 31, 0, 24));
        decision.setSourceReference("meeting:21;review:5");
        return researchDecisionRepository.save(decision);
    }
}
