package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.MilestoneUpdateRequest;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private TaskRepository taskRepository;

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

    @Test
    void updateRejectsATitleLongerThan255BeforeSaving() {
        Milestone existing = new Milestone();
        existing.setId(3L);
        existing.setProjectId(1L);
        existing.setTitle("Old title");
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
        MilestoneUpdateRequest request = new MilestoneUpdateRequest();
        request.setTitle("m".repeat(256));

        assertThatThrownBy(() -> milestoneService.updateMilestone(3L, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("at most 255 characters");
        verify(milestoneRepository, never()).save(existing);
    }

    @Test
    void explicitNullClearsWeightAndAnOmittedWeightDoesNot() {
        Milestone existing = new Milestone();
        existing.setId(3L);
        existing.setProjectId(1L);
        existing.setTitle("Baseline");
        existing.setWeight(new BigDecimal("2.50"));
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.save(existing)).thenReturn(existing);

        MilestoneUpdateRequest omitted = new MilestoneUpdateRequest();
        omitted.setTitle("Baseline");
        milestoneService.updateMilestone(3L, omitted);
        assertThat(existing.getWeight()).isEqualByComparingTo("2.50");

        MilestoneUpdateRequest cleared = new MilestoneUpdateRequest();
        cleared.setWeight(null);
        milestoneService.updateMilestone(3L, cleared);
        assertThat(existing.getWeight()).isNull();
    }

    @Test
    void deleteRejectsAMilestoneThatStillHasTasks() {
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(new Milestone()));
        when(taskRepository.findByMilestoneId(3L)).thenReturn(List.of(new Task()));

        assertThatThrownBy(() -> milestoneService.deleteMilestone(3L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> {
                ResponseStatusException exception = (ResponseStatusException) error;
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(exception.getReason()).contains("Reassign or remove those tasks first");
            });
        verify(milestoneRepository, never()).deleteById(3L);
    }

    @Test
    void deleteRemovesAMilestoneWithNoTasks() {
        when(milestoneRepository.findById(3L)).thenReturn(Optional.of(new Milestone()));
        when(taskRepository.findByMilestoneId(3L)).thenReturn(List.of());

        milestoneService.deleteMilestone(3L);

        verify(milestoneRepository).deleteById(3L);
    }
}
