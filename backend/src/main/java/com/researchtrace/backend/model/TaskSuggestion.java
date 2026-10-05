package com.researchtrace.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class TaskSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meetingId;

    private Long projectId;

    private String title;

    private String description;

    private String assigneeName;

    private Long assigneeId;

    private Long milestoneId;

    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String sourceExcerpt;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    private TaskSuggestionStatus status;

    private Long acceptedTaskId;

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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }

    public void setSourceExcerpt(String sourceExcerpt) {
        this.sourceExcerpt = sourceExcerpt;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public TaskSuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(TaskSuggestionStatus status) {
        this.status = status;
    }

    public Long getAcceptedTaskId() {
        return acceptedTaskId;
    }

    public void setAcceptedTaskId(Long acceptedTaskId) {
        this.acceptedTaskId = acceptedTaskId;
    }
}
