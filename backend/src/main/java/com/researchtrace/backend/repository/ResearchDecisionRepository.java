package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResearchDecisionRepository extends JpaRepository<ResearchDecision, Long> {

    List<ResearchDecision> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ResearchDecision> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, ResearchDecisionStatus status);

    List<ResearchDecision> findByMeetingId(Long meetingId);

    Optional<ResearchDecision> findBySupersedesDecisionId(Long supersedesDecisionId);
}
