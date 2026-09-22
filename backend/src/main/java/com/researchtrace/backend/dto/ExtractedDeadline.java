package com.researchtrace.backend.dto;

import java.time.LocalDate;

public class ExtractedDeadline {

    private String description;
    private LocalDate date;
    private String relativeDate;
    private String associatedTask;
    private Long source;
    private Double confidence;

    public ExtractedDeadline() {
    }

    public ExtractedDeadline(
            String description,
            LocalDate date,
            String relativeDate,
            String associatedTask,
            Long source,
            Double confidence) {
        this.description = description;
        this.date = date;
        this.relativeDate = relativeDate;
        this.associatedTask = associatedTask;
        this.source = source;
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getRelativeDate() {
        return relativeDate;
    }

    public void setRelativeDate(String relativeDate) {
        this.relativeDate = relativeDate;
    }

    public String getAssociatedTask() {
        return associatedTask;
    }

    public void setAssociatedTask(String associatedTask) {
        this.associatedTask = associatedTask;
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