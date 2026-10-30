package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByMilestoneId(Long milestoneId);

    List<Task> findByMilestoneIdIn(List<Long> milestoneIds);

    List<Task> findByAssigneeId(Long assigneeId);

    long countByMilestoneId(Long milestoneId);

    long countByMilestoneIdAndStatus(Long milestoneId, TaskStatus status);

    long countByAssigneeId(Long assigneeId);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
}