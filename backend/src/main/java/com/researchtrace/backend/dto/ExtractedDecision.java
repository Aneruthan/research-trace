package com.researchtrace.backend.dto;

public class ExtractedDecision {

    private String title;
    private String description;
    private String madeBy;
    private String reason;
    private Long source;
    private Double confidence;

    public ExtractedDecision() {
    }

    public ExtractedDecision(
            String title,
            String description,
            String madeBy,
            String reason,
            Long source,
            Double confidence) {
        this.title = title;
        this.description = description;
        this.madeBy = madeBy;
        this.reason = reason;
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

    public String getMadeBy() {
        return madeBy;
    }

    public void setMadeBy(String madeBy) {
        this.madeBy = madeBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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