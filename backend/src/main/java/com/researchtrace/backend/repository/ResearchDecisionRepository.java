package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResearchDecisionRepository extends JpaRepository<ResearchDecision, Long> {

    List<ResearchDecision> findByProjectIdOrderByDecidedAtAsc(Long projectId);

    List<ResearchDecision> findByProjectIdAndStatus(Long projectId, ResearchDecisionStatus status);

    List<ResearchDecision> findBySupersedesDecisionId(Long supersedesDecisionId);
}
