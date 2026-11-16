package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResearchEventRepository extends JpaRepository<ResearchEvent, Long> {

    boolean existsByProjectId(Long projectId);

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAsc(Long projectId);

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAscIdAsc(Long projectId);

    List<ResearchEvent> findByProjectIdAndEventType(Long projectId, ResearchEventType eventType);

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAscCreatedAtAsc(Long projectId);

    List<ResearchEvent> findByMeetingId(Long meetingId);

    List<ResearchEvent> findByResearchDecisionId(Long researchDecisionId);

    List<ResearchEvent> findByExperimentId(Long experimentId);

    List<ResearchEvent> findByTaskId(Long taskId);

    boolean existsByTaskIdAndEventType(Long taskId, ResearchEventType eventType);

    long countByProjectId(Long projectId);
}
