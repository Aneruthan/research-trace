package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
}