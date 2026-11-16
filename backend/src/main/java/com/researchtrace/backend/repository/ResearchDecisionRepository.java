package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResearchDecisionRepository extends JpaRepository<ResearchDecision, Long> {

    boolean existsByProjectId(Long projectId);

    List<ResearchDecision> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ResearchDecision> findByProjectIdOrderByDecidedAtAsc(Long projectId);

    List<ResearchDecision> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, ResearchDecisionStatus status);

    List<ResearchDecision> findByProjectIdAndStatus(Long projectId, ResearchDecisionStatus status);

    List<ResearchDecision> findByMeetingId(Long meetingId);

    List<ResearchDecision> findByExtractionReviewId(Long extractionReviewId);

    Optional<ResearchDecision> findBySupersedesDecisionId(Long supersedesDecisionId);

    List<ResearchDecision> findAllBySupersedesDecisionId(Long supersedesDecisionId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, ResearchDecisionStatus status);
}
