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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;
    private static final Long STUDENT_ID = 7L;
    private static final Long OTHER_STUDENT_ID = 8L;

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
    void returnsZeroWhenProjectHasNoMilestonesAndNoTasks() {
        givenMilestones();

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");
    }

    @Test
    void derivesMilestoneProgressFromCompletedTasks() {
        givenTaskCounts(11L, 2, 1);

        assertThat(progressService.getMilestoneProgress(11L))
            .isEqualByComparingTo("50.00");
    }

    @Test
    void returnsZeroMilestoneProgressWhenMilestoneHasNoTasks() {
        givenTaskCounts(11L, 0, 0);

        assertThat(progressService.getMilestoneProgress(11L))
            .isEqualByComparingTo("0.00");
    }

    @Test
    void doesNotCountIncompleteTasksTowardMilestoneProgress() {
        givenTaskCounts(11L, 2, 0);

        assertThat(progressService.getMilestoneProgress(11L))
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
    void isolatesTaskCompletionToEachMilestone() {
        givenMilestones(
            milestone(11L, "50"),
            milestone(12L, "50"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 4, 0);

        // Completing milestone 11 must not change milestone 12's 0.00 progress.
        // (50 * 100 + 50 * 0) / 100 = 50
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("50.00");

        verify(taskRepository).countByMilestoneIdAndProjectId(11L, PROJECT_ID);
        verify(taskRepository).countByMilestoneIdAndProjectId(12L, PROJECT_ID);
        verify(taskRepository).countByMilestoneIdAndProjectIdAndStatus(11L, PROJECT_ID, TaskStatus.COMPLETED);
        verify(taskRepository).countByMilestoneIdAndProjectIdAndStatus(12L, PROJECT_ID, TaskStatus.COMPLETED);
    }

    @Test
    void doesNotCountTodoOrInProgressTasksAsCompleted() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 3, 1);

        // 1 COMPLETED + 1 TODO + 1 IN_PROGRESS => 1/3
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("33.33");

        verify(taskRepository).countByMilestoneIdAndProjectIdAndStatus(11L, PROJECT_ID, TaskStatus.COMPLETED);
        verify(taskRepository, never())
            .countByMilestoneIdAndProjectIdAndStatus(eq(11L), eq(PROJECT_ID), eq(TaskStatus.TODO));
        verify(taskRepository, never())
            .countByMilestoneIdAndProjectIdAndStatus(eq(11L), eq(PROJECT_ID), eq(TaskStatus.IN_PROGRESS));
    }

    @Test
    void combinesTasksFromMultipleStudentsOnTheSameMilestone() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 2, 1);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("50.00");
    }

    @Test
    void returnsZeroWhenTotalWeightIsZeroAndNoMilestoneLessTasks() {
        givenMilestones(
            milestone(11L, "0"),
            milestone(12L, "0"));

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");

        verify(taskRepository, never()).countByMilestoneId(any());
    }

    @Test
    void returnsZeroWhenNoMilestoneHasAWeightAndNoMilestoneLessTasks() {
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

        verify(taskRepository, never()).countByMilestoneIdAndProjectId(eq(13L), any());
        verify(taskRepository, never()).countByMilestoneIdAndProjectId(eq(14L), any());
        verify(taskRepository, never()).countByMilestoneIdAndProjectId(eq(15L), any());
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

    @Test
    void calculatesStudentProgressFromCompletedAssignedTasks() {
        givenStudentTaskCounts(STUDENT_ID, 2, 1);

        assertThat(progressService.getStudentProgress(STUDENT_ID))
            .isEqualByComparingTo("50.00");
    }

    @Test
    void doesNotCountIncompleteTasksTowardStudentProgress() {
        givenStudentTaskCounts(STUDENT_ID, 3, 0);

        BigDecimal progress = progressService.getStudentProgress(STUDENT_ID);

        assertThat(progress).isEqualByComparingTo("0.00");
        verify(taskRepository).countByAssigneeIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED);
        verify(taskRepository, never())
            .countByAssigneeIdAndStatus(eq(STUDENT_ID), eq(TaskStatus.TODO));
        verify(taskRepository, never())
            .countByAssigneeIdAndStatus(eq(STUDENT_ID), eq(TaskStatus.IN_PROGRESS));
    }

    @Test
    void excludesTasksAssignedToOtherStudents() {
        givenStudentTaskCounts(STUDENT_ID, 2, 1);

        assertThat(progressService.getStudentProgress(STUDENT_ID))
            .isEqualByComparingTo("50.00");

        verify(taskRepository, never()).countByAssigneeId(OTHER_STUDENT_ID);
        verify(taskRepository, never())
            .countByAssigneeIdAndStatus(eq(OTHER_STUDENT_ID), any());
    }

    @Test
    void returnsZeroStudentProgressWhenStudentHasNoTasks() {
        givenStudentTaskCounts(STUDENT_ID, 0, 0);

        BigDecimal progress = progressService.getStudentProgress(STUDENT_ID);

        assertThat(progress).isEqualByComparingTo("0.00");
        assertThat(progress.scale()).isEqualTo(2);
    }

    @Test
    void roundsStudentProgressToTwoDecimals() {
        givenStudentTaskCounts(STUDENT_ID, 3, 2);

        BigDecimal progress = progressService.getStudentProgress(STUDENT_ID);

        assertThat(progress).isEqualByComparingTo("66.67");
        assertThat(progress.scale()).isEqualTo(2);
    }

    @Test
    void studentProgressDoesNotChangeProjectProgress() {
        givenMilestones(
            milestone(11L, "40"),
            milestone(12L, "60"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 2, 1);
        givenStudentTaskCounts(STUDENT_ID, 4, 1);

        assertThat(progressService.getStudentProgress(STUDENT_ID))
            .isEqualByComparingTo("25.00");
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("70.00");
    }

    @Test
    void projectProgressDoesNotUseStudentTaskCounts() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 2, 1);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("50.00");

        verify(taskRepository, never()).countByAssigneeId(any());
        verify(taskRepository, never()).countByAssigneeIdAndStatus(any(), any());
    }

    @Test
    void studentProgressDoesNotUseMilestones() {
        givenStudentTaskCounts(STUDENT_ID, 1, 1);

        assertThat(progressService.getStudentProgress(STUDENT_ID))
            .isEqualByComparingTo("100.00");

        verifyNoInteractions(milestoneService);
        verify(taskRepository, never()).countByMilestoneId(any());
    }

    @Test
    void isolatesTasksFromOtherProjectsInProjectProgress() {
        givenMilestones(milestone(11L, "100"));
        // Milestone 11 has only 1 task in PROJECT_ID (completed), but 2 total across all projects
        when(taskRepository.countByMilestoneIdAndProjectId(11L, PROJECT_ID)).thenReturn(1L);
        when(taskRepository.countByMilestoneIdAndProjectIdAndStatus(11L, PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(1L);
        lenient().when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(0L);

        // Progress for PROJECT_ID should be 100.00% regardless of other project tasks
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("100.00");

        verify(taskRepository).countByMilestoneIdAndProjectId(11L, PROJECT_ID);
        verify(taskRepository, never()).countByMilestoneIdAndProjectId(11L, OTHER_PROJECT_ID);
    }

    @Test
    void includesMilestoneLessTasksInProjectProgressWithMilestones() {
        givenMilestones(milestone(11L, "100"));
        // Milestone 11: 1 task, 1 completed (100%)
        givenTaskCounts(11L, 1, 1);
        // Project also has 1 milestone-less task (0 completed)
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(1L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(0L);

        // Combined: 1 completed milestone task (weight 100) + 1 uncompleted milestone-less task (weight 100) => 50.00%
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("50.00");
    }

    @Test
    void calculatesProjectProgressWithOnlyMilestoneLessTasks() {
        givenMilestones(); // No milestones
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(4L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(3L);

        // 3 of 4 completed => 75.00%
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("75.00");
    }

    @Test
    void milestonesWithZeroTasksAndMilestoneLessTasks() {
        givenMilestones(milestone(11L, "100"));
        givenTaskCounts(11L, 0, 0); // 0 tasks under milestone 11
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(1L);

        // Milestone 11 (weight 100, 0%) + Unassigned (weight 100, 50%) => (100*0 + 100*50)/200 = 25.00%
        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("25.00");
    }

    @Test
    void mixtureOfMultipleMilestonesAndMilestoneLessTasks() {
        givenMilestones(
            milestone(11L, "40"),
            milestone(12L, "60"));
        givenTaskCounts(11L, 2, 2); // 100%
        givenTaskCounts(12L, 2, 1); // 50%
        // Total milestone tasks = 4, total milestone weight = 100
        // Unassigned tasks: 2 tasks, 2 completed (100%)
        // unassignedWeight = 100 * 2 / 4 = 50
        // totalWeighted = 40*100 + 60*50 + 50*100 = 12000
        // totalWeight = 150 -> 12000/150 = 80.00%
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(2L);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("80.00");
    }

    @Test
    void allMilestoneTasksAndMilestoneLessTasksCompletedYields100Percent() {
        givenMilestones(
            milestone(11L, "50"),
            milestone(12L, "50"));
        givenTaskCounts(11L, 1, 1);
        givenTaskCounts(12L, 1, 1);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(2L);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("100.00");
    }

    @Test
    void allTasksIncompleteYieldsZeroPercent() {
        givenMilestones(
            milestone(11L, "50"),
            milestone(12L, "50"));
        givenTaskCounts(11L, 1, 0);
        givenTaskCounts(12L, 1, 0);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNull(PROJECT_ID)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(PROJECT_ID, TaskStatus.COMPLETED)).thenReturn(0L);

        assertThat(progressService.getProjectProgress(PROJECT_ID))
            .isEqualByComparingTo("0.00");
    }

    @Test
    void milestoneLevelProgressPreservesLegacyBehaviorWithoutProjectId() {
        when(taskRepository.countByMilestoneId(11L)).thenReturn(2L);
        when(taskRepository.countByMilestoneIdAndStatus(11L, TaskStatus.COMPLETED)).thenReturn(1L);

        assertThat(progressService.getMilestoneProgress(11L))
            .isEqualByComparingTo("50.00");
        verify(taskRepository).countByMilestoneId(11L);
    }

    private void givenStudentTaskCounts(Long studentId, long total, long completed) {
        when(taskRepository.countByAssigneeId(studentId)).thenReturn(total);
        if (total > 0) {
            when(taskRepository.countByAssigneeIdAndStatus(studentId, TaskStatus.COMPLETED))
                .thenReturn(completed);
        }
    }

    private void givenMilestones(Milestone... milestones) {
        when(milestoneService.getMilestonesByProject(PROJECT_ID))
            .thenReturn(List.of(milestones));
    }

    private void givenTaskCounts(Long milestoneId, long total, long completed) {
        lenient().when(taskRepository.countByMilestoneId(milestoneId)).thenReturn(total);
        lenient().when(taskRepository.countByMilestoneIdAndProjectId(milestoneId, PROJECT_ID)).thenReturn(total);
        if (total > 0) {
            lenient().when(taskRepository.countByMilestoneIdAndStatus(milestoneId, TaskStatus.COMPLETED))
                .thenReturn(completed);
            lenient().when(taskRepository.countByMilestoneIdAndProjectIdAndStatus(milestoneId, PROJECT_ID, TaskStatus.COMPLETED))
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
