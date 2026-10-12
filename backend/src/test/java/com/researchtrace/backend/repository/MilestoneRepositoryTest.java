package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MilestoneRepositoryTest {

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsWeight() {
        Milestone milestone = milestone(1L, "Baseline evaluation", new BigDecimal("40.00"));

        Long id = milestoneRepository.save(milestone).getId();
        entityManager.flush();
        entityManager.clear();

        Milestone reloaded = milestoneRepository.findById(id).orElseThrow();

        assertThat(reloaded.getWeight()).isEqualByComparingTo("40.00");
        assertThat(reloaded.getTitle()).isEqualTo("Baseline evaluation");
        assertThat(reloaded.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);
    }

    @Test
    void keepsWeightNullWhenNotProvided() {
        Milestone milestone = milestone(1L, "Error analysis", null);

        Long id = milestoneRepository.save(milestone).getId();
        entityManager.flush();
        entityManager.clear();

        Milestone reloaded = milestoneRepository.findById(id).orElseThrow();

        assertThat(reloaded.getWeight()).isNull();
    }

    @Test
    void findsMilestonesOfOneProjectOnly() {
        milestoneRepository.save(milestone(1L, "Baseline evaluation", new BigDecimal("40.00")));
        milestoneRepository.save(milestone(1L, "Error analysis", new BigDecimal("60.00")));
        milestoneRepository.save(milestone(2L, "Literature review", new BigDecimal("30.00")));
        entityManager.flush();
        entityManager.clear();

        List<Milestone> milestones = milestoneRepository.findByProjectId(1L);

        assertThat(milestones).hasSize(2);
        assertThat(milestones)
            .extracting(Milestone::getTitle)
            .containsExactlyInAnyOrder("Baseline evaluation", "Error analysis");
    }

    private Milestone milestone(Long projectId, String title, BigDecimal weight) {
        Milestone milestone = new Milestone();
        milestone.setProjectId(projectId);
        milestone.setTitle(title);
        milestone.setDescription("Milestone used by the progress tests");
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setDueDate(LocalDate.of(2026, 9, 25));
        milestone.setWeight(weight);
        return milestone;
    }
}
