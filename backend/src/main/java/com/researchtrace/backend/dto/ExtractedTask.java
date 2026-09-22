package com.researchtrace.backend.dto;

import java.time.LocalDate;

public class ExtractedTask {

    private String title;
    private String description;
    private Long assignee;
    private LocalDate deadline;
    private Long source;
    private Double confidence;

    public ExtractedTask() {
    }

    public ExtractedTask(
            String title,
            String description,
            Long assignee,
            LocalDate deadline,
            Long source,
            Double confidence) {
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.deadline = deadline;
        this.source = source;
        this.confidence = confidence;
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

    public Long getAssignee() {
        return assignee;
    }

    public void setAssignee(Long assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Long getSource() {
        return source;
    }

    public void setSource(Long source) {
        this.source = source;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}