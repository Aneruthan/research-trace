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
public class ResearchChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private Long meetingId;

    @Column(nullable = false)
    private String changeType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String previousApproach;

    @Column(columnDefinition = "TEXT")
    private String newApproach;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResearchChangeStatus status = ResearchChangeStatus.FLAGGED;

    @Column(nullable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPreviousApproach() {
        return previousApproach;
    }

    public void setPreviousApproach(String previousApproach) {
        this.previousApproach = previousApproach;
    }

    public String getNewApproach() {
        return newApproach;
    }

    public void setNewApproach(String newApproach) {
        this.newApproach = newApproach;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public ResearchChangeStatus getStatus() {
        return status;
    }

    public void setStatus(ResearchChangeStatus status) {
        this.status = status;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
