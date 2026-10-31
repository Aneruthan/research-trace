package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Task> findByMeetingId(Long meetingId);

    List<Task> findByTaskSuggestionId(Long taskSuggestionId);

    List<Task> findByResearchDecisionId(Long researchDecisionId);

    List<Task> findByExperimentId(Long experimentId);

    List<Task> findByProjectIdAndResearchDecisionId(Long projectId, Long researchDecisionId);

    List<Task> findByProjectIdAndExperimentId(Long projectId, Long experimentId);

    List<Task> findByMilestoneId(Long milestoneId);

    List<Task> findByMilestoneIdIn(List<Long> milestoneIds);

    List<Task> findByAssigneeId(Long assigneeId);

    long countByMilestoneId(Long milestoneId);

    long countByMilestoneIdAndStatus(Long milestoneId, TaskStatus status);

    long countByAssigneeId(Long assigneeId);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.milestoneId = :milestoneId AND (t.projectId = :projectId OR t.projectId IS NULL)")
    long countByMilestoneIdAndProjectId(@Param("milestoneId") Long milestoneId, @Param("projectId") Long projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.milestoneId = :milestoneId AND (t.projectId = :projectId OR t.projectId IS NULL) AND t.status = :status")
    long countByMilestoneIdAndProjectIdAndStatus(@Param("milestoneId") Long milestoneId, @Param("projectId") Long projectId, @Param("status") TaskStatus status);

    long countByProjectIdAndMilestoneIdIsNull(Long projectId);

    long countByProjectIdAndMilestoneIdIsNullAndStatus(Long projectId, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assigneeId = :assigneeId AND (t.projectId = :projectId OR t.projectId IS NULL)")
    long countByAssigneeIdAndProjectId(@Param("assigneeId") Long assigneeId, @Param("projectId") Long projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assigneeId = :assigneeId AND (t.projectId = :projectId OR t.projectId IS NULL) AND t.status = :status")
    long countByAssigneeIdAndProjectIdAndStatus(@Param("assigneeId") Long assigneeId, @Param("projectId") Long projectId, @Param("status") TaskStatus status);
}
