package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByProjectId(Long projectId);
}