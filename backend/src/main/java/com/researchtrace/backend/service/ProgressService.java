package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative progress calculation service.
 * Applies a single, consistent task-based contribution policy:
 * - COMPLETED   = 100% contribution
 * - IN_PROGRESS = 50% contribution
 * - TODO / other = 0% contribution
 * - No associated tasks = 0.00%
 *
 * Progress = sum(task contributions) / number of unique associated tasks.
 * All calculations are strictly bounded between 0.00% and 100.00%.
 */
@Service
public class ProgressService {

    public static final int PROGRESS_SCALE = 2;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    public static final BigDecimal FIFTY = new BigDecimal("50");

    private final MilestoneService milestoneService;
    private final TaskRepository taskRepository;

    public ProgressService(MilestoneService milestoneService, TaskRepository taskRepository) {
        this.milestoneService = milestoneService;
        this.taskRepository = taskRepository;
    }

    /**
     * Calculates project progress across all active project tasks.
     * Uses the unified 100/50/0 contribution policy.
     */
    public BigDecimal getProjectProgress(Long projectId) {
        if (projectId == null) {
            return zeroProgress();
        }
        List<Task> tasks = new ArrayList<>(taskRepository.findByProjectId(projectId));
        List<Milestone> milestones = milestoneService.getMilestonesByProject(projectId);
        if (milestones != null) {
            for (Milestone milestone : milestones) {
                if (milestone == null || milestone.getId() == null) {
                    continue;
                }
                List<Task> milestoneTasks = taskRepository.findByMilestoneId(milestone.getId());
                if (milestoneTasks == null) {
                    continue;
                }
                for (Task task : milestoneTasks) {
                    if (task != null && (task.getProjectId() == null || projectId.equals(task.getProjectId()))) {
                        tasks.add(task);
                    }
                }
            }
        }
        return calculateTaskProgress(tasks);
    }

    public BigDecimal getMilestoneProgress(Long milestoneId) {
        return getMilestoneProgress(milestoneId, null);
    }

    /**
     * Calculates milestone progress scoped to tasks within the milestone (and optional project).
     */
    public BigDecimal getMilestoneProgress(Long milestoneId, Long projectId) {
        if (milestoneId == null) {
            return zeroProgress();
        }
        List<Task> tasks;
        if (projectId != null) {
            tasks = taskRepository.findByMilestoneIdAndProjectId(milestoneId, projectId);
        } else {
            tasks = taskRepository.findByMilestoneId(milestoneId);
        }
        return calculateTaskProgress(tasks);
    }

    public BigDecimal getStudentProgress(Long studentId) {
        return getStudentProgress(studentId, null);
    }

    /**
     * Calculates student progress scoped to tasks assigned to the student (and optional project).
     */
    public BigDecimal getStudentProgress(Long studentId, Long projectId) {
        if (studentId == null) {
            return zeroProgress();
        }
        List<Task> tasks;
        if (projectId != null) {
            tasks = taskRepository.findByAssigneeIdAndProjectId(studentId, projectId);
        } else {
            tasks = taskRepository.findByAssigneeId(studentId);
        }
        return calculateTaskProgress(tasks);
    }

    /**
     * Unified task progress algorithm:
     * - Deduplicates tasks by ID
     * - COMPLETED = 100
     * - IN_PROGRESS = 50
     * - TODO / NOT_STARTED = 0
     * - Progress = sum(contributions) / count
     * - Bounded between 0.00 and 100.00
     */
    public BigDecimal calculateTaskProgress(Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return zeroProgress();
        }

        Map<Long, Task> uniqueTasks = new LinkedHashMap<>();
        long syntheticId = -1L;
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.getId() != null) {
                uniqueTasks.put(task.getId(), task);
            } else {
                uniqueTasks.put(syntheticId--, task);
            }
        }

        if (uniqueTasks.isEmpty()) {
            return zeroProgress();
        }

        BigDecimal totalPoints = BigDecimal.ZERO;
        for (Task task : uniqueTasks.values()) {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                totalPoints = totalPoints.add(ONE_HUNDRED);
            } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                totalPoints = totalPoints.add(FIFTY);
            }
        }

        BigDecimal progress = totalPoints.divide(
            BigDecimal.valueOf(uniqueTasks.size()),
            PROGRESS_SCALE,
            RoundingMode.HALF_UP
        );

        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            return zeroProgress();
        }
        if (progress.compareTo(ONE_HUNDRED) > 0) {
            return ONE_HUNDRED.setScale(PROGRESS_SCALE, RoundingMode.HALF_UP);
        }
        return progress;
    }

    public BigDecimal zeroProgress() {
        return BigDecimal.ZERO.setScale(PROGRESS_SCALE, RoundingMode.HALF_UP);
    }
}
