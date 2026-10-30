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
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private Long researchDecisionId;

    private Long meetingId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String hypothesis;

    @Column(columnDefinition = "TEXT")
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentStatus status = ExperimentStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String sourceReference;

    private LocalDate startedAt;

    private LocalDate completedAt;

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

    public Long getResearchDecisionId() {
        return researchDecisionId;
    }

    public void setResearchDecisionId(Long researchDecisionId) {
        this.researchDecisionId = researchDecisionId;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(String hypothesis) {
        this.hypothesis = hypothesis;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public LocalDate getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDate startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDate completedAt) {
        this.completedAt = completedAt;
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
