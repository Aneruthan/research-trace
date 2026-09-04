package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}