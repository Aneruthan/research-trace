package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResearchEventRepositoryTest {

    @Autowired
    private ResearchEventRepository researchEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void preservesChronologicalOrderOfResearchEvents() {
        ResearchEvent e1 = new ResearchEvent();
        e1.setProjectId(100L);
        e1.setEventType(ResearchEventType.MEETING);
        e1.setTitle("Initial Supervision Meeting");
        e1.setDescription("Discussed benchmark options for medical VQA.");
        e1.setOccurredAt(LocalDateTime.of(2026, 10, 10, 10, 0));
        e1.setCreatedAt(LocalDateTime.of(2026, 10, 10, 10, 0));
        e1.setMeetingId(200L);

        ResearchEvent e2 = new ResearchEvent();
        e2.setProjectId(100L);
        e2.setEventType(ResearchEventType.DECISION);
        e2.setTitle("Decision: Select PMC-VQA");
        e2.setOccurredAt(LocalDateTime.of(2026, 10, 10, 11, 0));
        e2.setCreatedAt(LocalDateTime.of(2026, 10, 10, 11, 0));
        e2.setResearchDecisionId(50L);

        ResearchEvent e3 = new ResearchEvent();
        e3.setProjectId(100L);
        e3.setEventType(ResearchEventType.EXPERIMENT);
        e3.setTitle("Experiment: Zero-shot Med-CLIP");
        e3.setOccurredAt(LocalDateTime.of(2026, 10, 12, 14, 0));
        e3.setCreatedAt(LocalDateTime.of(2026, 10, 12, 14, 0));
        e3.setExperimentId(70L);

        ResearchEvent e4 = new ResearchEvent();
        e4.setProjectId(100L);
        e4.setEventType(ResearchEventType.RESULT);
        e4.setTitle("Result: Low accuracy on pathology");
        e4.setOccurredAt(LocalDateTime.of(2026, 10, 16, 9, 0));
        e4.setCreatedAt(LocalDateTime.of(2026, 10, 16, 9, 0));
        e4.setResultId(90L);

        ResearchEvent e5 = new ResearchEvent();
        e5.setProjectId(100L);
        e5.setEventType(ResearchEventType.CHANGE);
        e5.setTitle("Change: Pivot to LitMedImage");
        e5.setOccurredAt(LocalDateTime.of(2026, 10, 17, 10, 0));
        e5.setCreatedAt(LocalDateTime.of(2026, 10, 17, 10, 0));
        e5.setResearchDecisionId(51L);

        // Save in mixed order
        researchEventRepository.save(e4);
        researchEventRepository.save(e2);
        researchEventRepository.save(e5);
        researchEventRepository.save(e1);
        researchEventRepository.save(e3);
        entityManager.flush();
        entityManager.clear();

        List<ResearchEvent> timeline = researchEventRepository
            .findByProjectIdOrderByOccurredAtAscCreatedAtAsc(100L);

        assertThat(timeline).hasSize(5);
        assertThat(timeline).extracting(ResearchEvent::getEventType)
            .containsExactly(
                ResearchEventType.MEETING,
                ResearchEventType.DECISION,
                ResearchEventType.EXPERIMENT,
                ResearchEventType.RESULT,
                ResearchEventType.CHANGE
            );
    }
}
