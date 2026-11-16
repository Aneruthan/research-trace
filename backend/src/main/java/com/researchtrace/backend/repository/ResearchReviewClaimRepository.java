package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ResearchReviewClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchReviewClaimRepository extends JpaRepository<ResearchReviewClaim, Long> {

    boolean existsByMeetingIdAndContentHash(Long meetingId, String contentHash);

    void deleteByExtractionReviewId(Long extractionReviewId);
}
