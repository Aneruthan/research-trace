package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.repository.MilestoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private MilestoneService milestoneService;

    @Test
    void updatePersistsEditedFieldsAndKeepsTheProject() {
        Milestone existing = new Milestone();
        existing.setId(3L);
        existing.setProjectId(1L);
        existing.setTitle("Old title");
        existing.setWeight(new BigDecimal("1.00"));
        existing.setStatus(MilestoneStatus.NOT_STARTED);
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.save(existing)).thenReturn(existing);

        Milestone patch = new Milestone();
        patch.setProjectId(1L);
        patch.setTitle("Revised title");
        patch.setDescription("Updated scope");
        patch.setStatus(MilestoneStatus.IN_PROGRESS);
        patch.setWeight(new BigDecimal("2.50"));
        patch.setDueDate(LocalDate.of(2026, 11, 20));

        Milestone saved = milestoneService.updateMilestone(3L, patch);

        assertThat(saved.getTitle()).isEqualTo("Revised title");
        assertThat(saved.getDescription()).isEqualTo("Updated scope");
        assertThat(saved.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);
        assertThat(saved.getWeight()).isEqualByComparingTo("2.50");
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(saved.getProjectId()).isEqualTo(1L);
        verify(milestoneRepository).save(existing);
    }

    @Test
    void updateRejectsAnEmptyTitle() {
        Milestone existing = new Milestone();
        existing.setId(3L);
        existing.setProjectId(1L);
        existing.setTitle("Old title");
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
        Milestone patch = new Milestone();
        patch.setTitle("   ");

        assertThatThrownBy(() -> milestoneService.updateMilestone(3L, patch))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("title cannot be empty");
    }

    @Test
    void updateRejectsANegativeWeight() {
        Milestone existing = new Milestone();
        existing.setId(3L);
        existing.setProjectId(1L);
        existing.setTitle("Old title");
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
        Milestone patch = new Milestone();
        patch.setWeight(new BigDecimal("-1"));

        assertThatThrownBy(() -> milestoneService.updateMilestone(3L, patch))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("cannot be negative");
    }

    @Test
    void updateRejectsAMissingMilestone() {
        when(milestoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.updateMilestone(99L, new Milestone()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not found");
    }
}
