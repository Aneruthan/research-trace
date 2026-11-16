package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TaskSuggestionRepository extends JpaRepository<TaskSuggestion, Long> {

    List<TaskSuggestion> findByMeetingId(Long meetingId);

    List<TaskSuggestion> findByAcceptedTaskId(Long acceptedTaskId);

    List<TaskSuggestion> findByProjectIdAndStatus(Long projectId, TaskSuggestionStatus status);

    boolean existsByProjectId(Long projectId);

    @Transactional
    void deleteByMeetingIdAndStatus(Long meetingId, TaskSuggestionStatus status);
}
