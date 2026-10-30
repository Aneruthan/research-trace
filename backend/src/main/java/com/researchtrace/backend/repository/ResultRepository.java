package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Result> findByExperimentId(Long experimentId);

    List<Result> findByMeetingId(Long meetingId);
}
