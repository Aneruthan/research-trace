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

        for (Milestone milestone : milestones) {
            BigDecimal weight = valueOrZero(milestone.getWeight());
            if (weight.signum() <= 0) {
                continue;
            }

            BigDecimal progress = milestoneProgress(milestone.getId());

            weightedProgress = weightedProgress.add(weight.multiply(progress));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.signum() == 0) {
            return zeroProgress();
        }

        return weightedProgress.divide(totalWeight, PROGRESS_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal milestoneProgress(Long milestoneId) {
        long totalTasks = taskRepository.countByMilestoneId(milestoneId);
        if (totalTasks == 0) {
            return zeroProgress();
        }

        long completedTasks = taskRepository.countByMilestoneIdAndStatus(
            milestoneId,
            TaskStatus.COMPLETED);

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
}
