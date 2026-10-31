package com.researchtrace.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_research_review_claim_meeting_hash",
    columnNames = {"meeting_id", "content_hash"}))
public class ResearchReviewClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private Long extractionReviewId;

    @Column(nullable = false, length = 64)
    private String contentHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Long getExtractionReviewId() {
        return extractionReviewId;
    }

    public void setExtractionReviewId(Long extractionReviewId) {
        this.extractionReviewId = extractionReviewId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}
