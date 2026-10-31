package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProgressService {

    private static final int PROGRESS_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final MilestoneService milestoneService;

    private final TaskRepository taskRepository;

    public ProgressService(MilestoneService milestoneService, TaskRepository taskRepository) {
        this.milestoneService = milestoneService;
        this.taskRepository = taskRepository;
    }

    public BigDecimal getProjectProgress(Long projectId) {
        List<Milestone> milestones = milestoneService.getMilestonesByProject(projectId);

        BigDecimal weightedProgress = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        long milestoneTasks = 0;

        for (Milestone milestone : milestones) {
            BigDecimal weight = valueOrZero(milestone.getWeight());
            if (weight.signum() <= 0) {
                continue;
            }

            MilestoneProgressResult result = calculateMilestoneProgress(milestone.getId(), projectId);

            weightedProgress = weightedProgress.add(weight.multiply(result.progress()));
            totalWeight = totalWeight.add(weight);
            milestoneTasks += result.totalTasks();
        }

        long unassignedTotal = taskRepository.countByProjectIdAndMilestoneIdIsNull(projectId);
        long unassignedCompleted = taskRepository.countByProjectIdAndMilestoneIdIsNullAndStatus(
            projectId,
            TaskStatus.COMPLETED);

        if (totalWeight.signum() == 0) {
            if (unassignedTotal == 0) {
                return zeroProgress();
            }
            return percentage(unassignedCompleted, unassignedTotal);
        }

        if (unassignedTotal == 0) {
            return weightedProgress.divide(totalWeight, PROGRESS_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal unassignedWeight = milestoneTasks > 0
            ? totalWeight.multiply(BigDecimal.valueOf(unassignedTotal))
                .divide(BigDecimal.valueOf(milestoneTasks), 4, RoundingMode.HALF_UP)
            : totalWeight;
        BigDecimal unassignedProgress = percentage(unassignedCompleted, unassignedTotal);
        BigDecimal combinedWeight = totalWeight.add(unassignedWeight);
        BigDecimal combinedProgress = weightedProgress.add(unassignedWeight.multiply(unassignedProgress));

        return combinedProgress.divide(combinedWeight, PROGRESS_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getMilestoneProgress(Long milestoneId) {
        return getMilestoneProgress(milestoneId, null);
    }

    public BigDecimal getMilestoneProgress(Long milestoneId, Long projectId) {
        return calculateMilestoneProgress(milestoneId, projectId).progress();
    }

    private MilestoneProgressResult calculateMilestoneProgress(Long milestoneId, Long projectId) {
        long totalTasks;
        long completedTasks;

        if (projectId != null) {
            totalTasks = taskRepository.countByMilestoneIdAndProjectId(milestoneId, projectId);
            if (totalTasks == 0) {
                return new MilestoneProgressResult(zeroProgress(), 0);
            }
            completedTasks = taskRepository.countByMilestoneIdAndProjectIdAndStatus(
                milestoneId,
                projectId,
                TaskStatus.COMPLETED);
        } else {
            totalTasks = taskRepository.countByMilestoneId(milestoneId);
            if (totalTasks == 0) {
                return new MilestoneProgressResult(zeroProgress(), 0);
            }
            completedTasks = taskRepository.countByMilestoneIdAndStatus(
                milestoneId,
                TaskStatus.COMPLETED);
        }

        return new MilestoneProgressResult(percentage(completedTasks, totalTasks), totalTasks);
    }

    public BigDecimal getStudentProgress(Long studentId) {
        return getStudentProgress(studentId, null);
    }

    public BigDecimal getStudentProgress(Long studentId, Long projectId) {
        long totalTasks;
        long completedTasks;

        if (projectId != null) {
            totalTasks = taskRepository.countByAssigneeIdAndProjectId(studentId, projectId);
            if (totalTasks == 0) {
                return zeroProgress();
            }
            completedTasks = taskRepository.countByAssigneeIdAndProjectIdAndStatus(
                studentId,
                projectId,
                TaskStatus.COMPLETED);
        } else {
            totalTasks = taskRepository.countByAssigneeId(studentId);
            if (totalTasks == 0) {
                return zeroProgress();
            }
            completedTasks = taskRepository.countByAssigneeIdAndStatus(
                studentId,
                TaskStatus.COMPLETED);
        }

        return percentage(completedTasks, totalTasks);
    }

    private BigDecimal percentage(long completedTasks, long totalTasks) {
        return BigDecimal.valueOf(completedTasks)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(totalTasks), PROGRESS_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zeroProgress() {
        return BigDecimal.ZERO.setScale(PROGRESS_SCALE, RoundingMode.HALF_UP);
    }

    private record MilestoneProgressResult(BigDecimal progress, long totalTasks) {
    }
}
