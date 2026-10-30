package com.researchtrace.backend.service;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ResearchDecisionSupersessionRollbackTest.FailingSupersessionConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ResearchDecisionSupersessionRollbackTest {

    @Autowired
    private ResearchDecisionService researchDecisionService;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    private Long previousId;
    private Long replacementId;

    @BeforeEach
    void setUp() {
        researchDecisionRepository.deleteAll();
        previousId = save("Keep the baseline").getId();
        replacementId = save("Use a balanced split").getId();
    }

    @Test
    void failedSupersessionRollsBackBothDecisions() {
        assertThatThrownBy(() -> researchDecisionService.supersede(1L, previousId, replacementId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("supersession failed");

        assertThat(researchDecisionRepository.count()).isEqualTo(2);
        ResearchDecision previous = researchDecisionRepository.findById(previousId).orElseThrow();
        ResearchDecision replacement = researchDecisionRepository.findById(replacementId).orElseThrow();
        assertThat(previous.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(previous.getTitle()).isEqualTo("Keep the baseline");
        assertThat(replacement.getStatus()).isEqualTo(ResearchDecisionStatus.ACTIVE);
        assertThat(replacement.getSupersedesDecisionId()).isNull();
    }

    private ResearchDecision save(String title) {
        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(1L);
        decision.setTitle(title);
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        return researchDecisionRepository.save(decision);
    }

    static class FailingSupersessionConfig {
        @Bean
        ResearchDecisionService researchDecisionService(ResearchDecisionRepository researchDecisionRepository) {
            return new ResearchDecisionService(researchDecisionRepository) {
                @Override
                protected void persist(ResearchDecision previous, ResearchDecision replacement) {
                    previous.setStatus(ResearchDecisionStatus.SUPERSEDED);
                    replacement.setSupersedesDecisionId(previous.getId());
                    researchDecisionRepository.saveAndFlush(previous);
                    researchDecisionRepository.saveAndFlush(replacement);
                    throw new IllegalStateException("supersession failed");
                }
            };
        }
    }
}
