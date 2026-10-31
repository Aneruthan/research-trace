package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResearchChangeRepository extends JpaRepository<ResearchChange, Long> {

    List<ResearchChange> findByProjectIdOrderByDetectedAtDesc(Long projectId);

    List<ResearchChange> findByProjectIdAndStatusOrderByDetectedAtDesc(Long projectId, ResearchChangeStatus status);

    List<ResearchChange> findByMeetingId(Long meetingId);
}
