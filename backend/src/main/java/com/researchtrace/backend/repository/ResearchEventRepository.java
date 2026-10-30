package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResearchEventRepository extends JpaRepository<ResearchEvent, Long> {

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAscCreatedAtAsc(Long projectId);

    List<ResearchEvent> findByMeetingId(Long meetingId);

    List<ResearchEvent> findByResearchDecisionId(Long researchDecisionId);

    List<ResearchEvent> findByExperimentId(Long experimentId);
}
