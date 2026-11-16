package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    List<Experiment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Experiment> findByResearchDecisionId(Long researchDecisionId);

    List<Experiment> findByMeetingId(Long meetingId);

    long countByProjectId(Long projectId);
}
