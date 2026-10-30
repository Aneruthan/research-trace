package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ResearchChangeRepositoryTest {

    @Autowired
    private ResearchChangeRepository researchChangeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndFiltersResearchChangesByStatus() {
        ResearchChange change1 = new ResearchChange();
        change1.setProjectId(100L);
        change1.setMeetingId(201L);
        change1.setChangeType("DATASET");
        change1.setTitle("Dataset switch: PMC-VQA to LitMedImage");
        change1.setPreviousApproach("PMC-VQA dataset");
        change1.setNewApproach("LitMedImage dataset");
        change1.setReason("High annotation noise in PMC-VQA labels.");
        change1.setSourceReference("Meeting Oct 17 notes: supervisor suggested switching.");
        change1.setStatus(ResearchChangeStatus.FLAGGED);
        change1.setDetectedAt(LocalDateTime.of(2026, 10, 17, 10, 45));

        ResearchChange change2 = new ResearchChange();
        change2.setProjectId(100L);
        change2.setMeetingId(202L);
        change2.setChangeType("MODEL");
        change2.setTitle("Model upgrade: ViT-B to ViT-L");
        change2.setStatus(ResearchChangeStatus.REVIEWED);
        change2.setDetectedAt(LocalDateTime.of(2026, 10, 24, 11, 0));

        researchChangeRepository.save(change1);
        researchChangeRepository.save(change2);
        entityManager.flush();
        entityManager.clear();

        List<ResearchChange> flagged = researchChangeRepository
            .findByProjectIdAndStatusOrderByDetectedAtDesc(100L, ResearchChangeStatus.FLAGGED);
        assertThat(flagged).hasSize(1);
        assertThat(flagged.get(0).getTitle()).isEqualTo("Dataset switch: PMC-VQA to LitMedImage");
        assertThat(flagged.get(0).getChangeType()).isEqualTo("DATASET");

        List<ResearchChange> all = researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(100L);
        assertThat(all).hasSize(2);
    }
}
