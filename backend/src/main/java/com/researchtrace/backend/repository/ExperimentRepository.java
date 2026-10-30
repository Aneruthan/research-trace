package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByProjectId(Long projectId);

    List<Experiment> findByResearchDecisionId(Long researchDecisionId);
}
