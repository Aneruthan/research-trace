package com.researchtrace.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String extractionJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
