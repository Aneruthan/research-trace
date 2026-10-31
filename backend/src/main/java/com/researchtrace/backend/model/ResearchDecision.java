package com.researchtrace.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class ResearchDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private Long meetingId;

    private Long extractionReviewId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String decisionText;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResearchDecisionStatus status = ResearchDecisionStatus.ACTIVE;

    @Column(unique = true)
    private Long supersedesDecisionId;

    @Column(columnDefinition = "TEXT")
    private String sourceReference;

    private LocalDate decidedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private Long createdByUserId;

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

    public Long getExtractionReviewId() {
        return extractionReviewId;
    }

    public void setExtractionReviewId(Long extractionReviewId) {
        this.extractionReviewId = extractionReviewId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDecisionText() {
        return decisionText;
    }

    public void setDecisionText(String decisionText) {
        this.decisionText = decisionText;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public ResearchDecisionStatus getStatus() {
        return status;
    }

    public void setStatus(ResearchDecisionStatus status) {
        this.status = status;
    }

    public Long getSupersedesDecisionId() {
        return supersedesDecisionId;
    }

    public void setSupersedesDecisionId(Long supersedesDecisionId) {
        this.supersedesDecisionId = supersedesDecisionId;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public LocalDate getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDate decidedAt) {
        this.decidedAt = decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt == null ? null : decidedAt.toLocalDate();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
