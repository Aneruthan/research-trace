package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByTaskIdOrderByChangedAtAscIdAsc(Long taskId);

    List<TaskHistory> findByTaskId(Long taskId);
}
