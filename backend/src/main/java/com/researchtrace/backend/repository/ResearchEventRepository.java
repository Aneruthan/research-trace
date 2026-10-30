package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResearchEventRepository extends JpaRepository<ResearchEvent, Long> {

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAsc(Long projectId);

    List<ResearchEvent> findByProjectIdOrderByOccurredAtAscIdAsc(Long projectId);

    List<ResearchEvent> findByProjectIdAndEventType(Long projectId, ResearchEventType eventType);
}
