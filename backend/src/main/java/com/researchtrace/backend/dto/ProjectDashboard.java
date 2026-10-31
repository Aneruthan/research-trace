package com.researchtrace.backend.dto;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Project;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskSuggestion;

import java.math.BigDecimal;
import java.util.List;

public record ProjectDashboard(
        Project project,
        BigDecimal projectProgress,
        List<ProjectMember> members,
        List<StudentProgressItem> studentProgress,
        List<Milestone> milestones,
        List<MilestoneProgressItem> milestoneProgress,
        List<Meeting> meetings,
        List<Task> tasks,
        List<TaskSuggestion> pendingTaskSuggestions,
        ResearchMetrics researchMetrics) {

    public ProjectDashboard(
            Project project,
            BigDecimal projectProgress,
            List<ProjectMember> members,
            List<StudentProgressItem> studentProgress,
            List<Milestone> milestones,
            List<MilestoneProgressItem> milestoneProgress,
            List<Meeting> meetings,
            List<Task> tasks,
            List<TaskSuggestion> pendingTaskSuggestions) {
        this(project, projectProgress, members, studentProgress, milestones, milestoneProgress, meetings, tasks, pendingTaskSuggestions, null);
    }

    public record StudentProgressItem(Long studentId, BigDecimal progress) {
    }

    public record MilestoneProgressItem(Long milestoneId, BigDecimal progress) {
    }

    public record ResearchMetrics(
            long totalDecisions,
            long activeDecisions,
            long totalExperiments,
            long totalResults,
            long totalEvents,
            long totalChanges) {
    }
}
