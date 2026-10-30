package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExperimentRepositoryTest {

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndRetrievesExperimentsLinkedToDecision() {
        Experiment exp1 = new Experiment();
        exp1.setProjectId(100L);
        exp1.setResearchDecisionId(50L);
        exp1.setMeetingId(200L);
        exp1.setTitle("Baseline evaluation on PMC-VQA");
        exp1.setHypothesis("Zero-shot Med-CLIP achieves >65% accuracy on closed questions.");
        exp1.setMethod("Evaluate pretrained weights with prompt template standard.");
        exp1.setStatus(ExperimentStatus.COMPLETED);
        exp1.setSourceReference("Meeting Oct 10");
        exp1.setStartedAt(LocalDate.of(2026, 10, 11));
        exp1.setCompletedAt(LocalDate.of(2026, 10, 13));
        exp1.setCreatedAt(LocalDateTime.of(2026, 10, 10, 15, 0));

        Experiment exp2 = new Experiment();
        exp2.setProjectId(100L);
        exp2.setResearchDecisionId(50L);
        exp2.setTitle("Fine-tuning on PMC-VQA train split");
        exp2.setStatus(ExperimentStatus.RUNNING);
        exp2.setCreatedAt(LocalDateTime.of(2026, 10, 14, 10, 0));

        Experiment other = new Experiment();
        other.setProjectId(100L);
        other.setResearchDecisionId(51L);
        other.setTitle("Unrelated experiment");
        other.setStatus(ExperimentStatus.PLANNED);
        other.setCreatedAt(LocalDateTime.of(2026, 10, 15, 10, 0));

        experimentRepository.save(exp1);
        experimentRepository.save(exp2);
        experimentRepository.save(other);
        entityManager.flush();
        entityManager.clear();

        List<Experiment> forDecision = experimentRepository.findByResearchDecisionId(50L);
        assertThat(forDecision).hasSize(2);
        assertThat(forDecision).extracting(Experiment::getTitle)
            .containsExactlyInAnyOrder("Baseline evaluation on PMC-VQA", "Fine-tuning on PMC-VQA train split");

        List<Experiment> forProject = experimentRepository.findByProjectIdOrderByCreatedAtDesc(100L);
        assertThat(forProject).hasSize(3);
    }
}
