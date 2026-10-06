package com.researchtrace.backend.model;

import jakarta.persistence.*;

@Entity
public class ExtractionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionReviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String extractionJson;

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

    public ExtractionReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ExtractionReviewStatus status) {
        this.status = status;
    }

    public String getExtractionJson() {
        return extractionJson;
    }

    public void setExtractionJson(String extractionJson) {
        this.extractionJson = extractionJson;
    }
}