package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void calculatesWeightedAverageFromTaskCompletion() {
        givenMilestones(
            milestone(11L, "40"),
            milestone(12L, "60"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 2, 1);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("70.00");
    }

    @Test
    void weighsEachMilestoneByItsOwnWeight() {
        givenMilestones(
            milestone(11L, "20"),
            milestone(12L, "30"),
            milestone(13L, "50"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 5, 2);
        givenTaskCounts(13L, 1, 0);

        // (20 * 100 + 30 * 40 + 50 * 0) / 100 = 32
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("32.00");
    }

    @Test
    void returnsZeroWhenProjectHasNoMilestones() {
        givenMilestones();

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");
    }

    @Test
    void returnsZeroWhenMilestoneHasNoTasks() {
        givenMilestones(milestone(11L, "40"));
        givenTaskCounts(11L, 0, 0);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");
    }

    @Test
    void doesNotCountTodoOrInProgressTasksAsCompleted() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 3, 1);

        // 1 COMPLETED + 1 TODO + 1 IN_PROGRESS => 1/3
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("33.33");

        verify(taskRepository).countByMilestoneIdAndStatus(11L, TaskStatus.COMPLETED);
        verify(taskRepository, never())
            .countByMilestoneIdAndStatus(eq(11L), eq(TaskStatus.TODO));
        verify(taskRepository, never())
            .countByMilestoneIdAndStatus(eq(11L), eq(TaskStatus.IN_PROGRESS));
    }

    @Test
    void combinesTasksFromMultipleStudentsOnTheSameMilestone() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 2, 1);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("50.00");
    }

    @Test
    void returnsZeroWhenTotalWeightIsZero() {
        givenMilestones(
            milestone(11L, "0"),
            milestone(12L, "0"));

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");

        verify(taskRepository, never()).countByMilestoneId(any());
    }

    @Test
    void returnsZeroWhenNoMilestoneHasAWeight() {
        givenMilestones(
            milestone(11L, null),
            milestone(12L, null));

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");

        verify(taskRepository, never()).countByMilestoneId(any());
    }

    @Test
    void ignoresNullZeroAndNegativeWeights() {
        givenMilestones(
            milestone(11L, "40"),
            milestone(12L, "60"),
            milestone(13L, null),
            milestone(14L, "0"),
            milestone(15L, "-10"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 0, 0);

        // (40 * 100 + 60 * 0) / 100 = 40; invalid weights are skipped
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("40.00");

        verify(taskRepository, never()).countByMilestoneId(13L);
        verify(taskRepository, never()).countByMilestoneId(14L);
        verify(taskRepository, never()).countByMilestoneId(15L);
    }

    @Test
    void roundsOneThirdToTwoDecimals() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 3, 1);

        BigDecimal progress = progressService.getProjectProgress(PROJECT_ID);

        assertThat(progress).isEqualByComparingTo("33.33");
        assertThat(progress.scale()).isEqualTo(2);
    }

    @Test
    void calculatesWeightedProjectProgressFromRoundedMilestoneProgress() {
        givenMilestones(
            milestone(11L, "20"),
            milestone(12L, "30"),
            milestone(13L, "50"));
        givenTaskCounts(11L, 3, 1);
        givenTaskCounts(12L, 3, 2);
        givenTaskCounts(13L, 1, 0);

        // (20 * 33.33 + 30 * 66.67 + 50 * 0) / 100 = 26.667 -> 26.67
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("26.67");
    }

    private void givenMilestones(Milestone... milestones) {
        when(milestoneService.getMilestonesByProject(PROJECT_ID))
            .thenReturn(List.of(milestones));
    }

    private void givenTaskCounts(Long milestoneId, long total, long completed) {
        when(taskRepository.countByMilestoneId(milestoneId)).thenReturn(total);
        if (total > 0) {
            when(taskRepository.countByMilestoneIdAndStatus(milestoneId, TaskStatus.COMPLETED))
                .thenReturn(completed);
        }
    }

    private Milestone milestone(Long id, String weight) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProjectId(PROJECT_ID);
        milestone.setTitle("Milestone");
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setWeight(weight == null ? null : new BigDecimal(weight));
        return milestone;
    }
}
