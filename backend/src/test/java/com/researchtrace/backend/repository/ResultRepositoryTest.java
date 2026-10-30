package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResultRepositoryTest {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndRetrievesResultsLinkedToExperiment() {
        Result res1 = new Result();
        res1.setProjectId(100L);
        res1.setExperimentId(70L);
        res1.setMeetingId(201L);
        res1.setSummary("Baseline Med-CLIP achieved 58.4% accuracy, below expected 65%.");
        res1.setDataOrMetric("Accuracy: 58.4%, F1: 0.54");
        res1.setComparison("Underperforms ClinicalBERT by 3.2% on pathology subset.");
        res1.setSourceReference("Meeting Oct 17 notes: discussed evaluation results.");
        res1.setObservedAt(LocalDate.of(2026, 10, 16));
        res1.setCreatedAt(LocalDateTime.of(2026, 10, 17, 10, 30));

        Result res2 = new Result();
        res2.setProjectId(100L);
        res2.setExperimentId(70L);
        res2.setSummary("Ablation without text prompt achieved 42.1% accuracy.");
        res2.setDataOrMetric("Accuracy: 42.1%");
        res2.setCreatedAt(LocalDateTime.of(2026, 10, 17, 11, 0));

        resultRepository.save(res1);
        resultRepository.save(res2);
        entityManager.flush();
        entityManager.clear();

        List<Result> forExperiment = resultRepository.findByExperimentId(70L);
        assertThat(forExperiment).hasSize(2);
        assertThat(forExperiment).extracting(Result::getDataOrMetric)
            .containsExactlyInAnyOrder("Accuracy: 58.4%, F1: 0.54", "Accuracy: 42.1%");

        List<Result> forMeeting = resultRepository.findByMeetingId(201L);
        assertThat(forMeeting).hasSize(1);
        assertThat(forMeeting.get(0).getSummary()).contains("58.4% accuracy");
    }
}
