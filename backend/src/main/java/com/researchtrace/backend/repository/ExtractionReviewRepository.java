package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ExtractionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractionReviewRepository extends JpaRepository<ExtractionReview, Long> {

    List<ExtractionReview> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
