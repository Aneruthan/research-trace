package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResearchDecisionRepositoryTest {

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndRetrievesResearchDecisionWithProvenance() {
        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(100L);
        decision.setMeetingId(200L);
        decision.setTitle("Adopt PMC-VQA benchmark");
        decision.setDecisionText("Use PMC-VQA for medical visual question answering evaluation.");
        decision.setRationale("PMC-VQA is the de facto standard in recent literature.");
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        decision.setSourceReference("Meeting Oct 10: discussed benchmark datasets.");
        decision.setDecidedAt(LocalDate.of(2026, 10, 10));
        decision.setCreatedAt(LocalDateTime.of(2026, 10, 10, 14, 0));

        Long id = researchDecisionRepository.save(decision).getId();
        entityManager.flush();
        entityManager.clear();

        ResearchDecision found = researchDecisionRepository.findById(id).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Adopt PMC-VQA benchmark");
        assertThat(found.getProjectId()).isEqualTo(100L);
        assertThat(found.getMeetingId()).isEqualTo(200L);
        assertThat(found.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(found.getSourceReference()).contains("discussed benchmark datasets");
        assertThat(found.getSupersedesDecisionId()).isNull();
    }

    @Test
    void tracksDecisionSupersessionLineage() {
        // Initial decision
        ResearchDecision initialDecision = new ResearchDecision();
        initialDecision.setProjectId(100L);
        initialDecision.setTitle("Use PMC-VQA");
        initialDecision.setStatus(ResearchDecisionStatus.ACTIVE);
        initialDecision.setCreatedAt(LocalDateTime.of(2026, 10, 10, 10, 0));
        Long initialId = researchDecisionRepository.save(initialDecision).getId();

        entityManager.flush();
        entityManager.clear();

        // Later meeting: supersede decision with LitMedImage
        ResearchDecision initialReloaded = researchDecisionRepository.findById(initialId).orElseThrow();
        initialReloaded.setStatus(ResearchDecisionStatus.SUPERSEDED);
        researchDecisionRepository.save(initialReloaded);

        ResearchDecision newDecision = new ResearchDecision();
        newDecision.setProjectId(100L);
        newDecision.setTitle("Switch to LitMedImage dataset");
        newDecision.setDecisionText("Pivot from PMC-VQA to LitMedImage due to cleaner clinical annotations.");
        newDecision.setRationale("PMC-VQA had noisy labels observed in experiment 1.");
        newDecision.setStatus(ResearchDecisionStatus.ACTIVE);
        newDecision.setSupersedesDecisionId(initialId);
        newDecision.setCreatedAt(LocalDateTime.of(2026, 10, 24, 11, 0));
        researchDecisionRepository.save(newDecision);

        entityManager.flush();
        entityManager.clear();

        // Verify active decisions only return the new one
        List<ResearchDecision> active = researchDecisionRepository
            .findByProjectIdAndStatusOrderByCreatedAtDesc(100L, ResearchDecisionStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getTitle()).isEqualTo("Switch to LitMedImage dataset");
        assertThat(active.get(0).getSupersedesDecisionId()).isEqualTo(initialId);

        // Verify the superseded decision still exists and is marked SUPERSEDED
        ResearchDecision superseded = researchDecisionRepository.findById(initialId).orElseThrow();
        assertThat(superseded.getStatus()).isEqualTo(ResearchDecisionStatus.SUPERSEDED);

        // Verify lookup by supersedesDecisionId
        Optional<ResearchDecision> successor = researchDecisionRepository.findBySupersedesDecisionId(initialId);
        assertThat(successor).isPresent();
        assertThat(successor.get().getTitle()).isEqualTo("Switch to LitMedImage dataset");
    }

    @Test
    void retrievesDecisionsByMeetingId() {
        ResearchDecision d1 = new ResearchDecision();
        d1.setProjectId(100L);
        d1.setMeetingId(200L);
        d1.setTitle("Decision in Meeting 200");
        d1.setStatus(ResearchDecisionStatus.ACTIVE);
        d1.setCreatedAt(LocalDateTime.of(2026, 10, 10, 10, 0));

        ResearchDecision d2 = new ResearchDecision();
        d2.setProjectId(100L);
        d2.setMeetingId(201L);
        d2.setTitle("Decision in Meeting 201");
        d2.setStatus(ResearchDecisionStatus.ACTIVE);
        d2.setCreatedAt(LocalDateTime.of(2026, 10, 17, 10, 0));

        researchDecisionRepository.save(d1);
        researchDecisionRepository.save(d2);
        entityManager.flush();
        entityManager.clear();

        List<ResearchDecision> meeting200Decisions = researchDecisionRepository.findByMeetingId(200L);
        assertThat(meeting200Decisions).hasSize(1);
        assertThat(meeting200Decisions.get(0).getTitle()).isEqualTo("Decision in Meeting 200");
    }
}
