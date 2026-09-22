package com.researchtrace.backend.dto;

public class ExtractedResearchChange {

    private String description;
    private String previousApproach;
    private String newApproach;
    private String reason;
    private Long source;
    private Double confidence;

    public ExtractedResearchChange() {
    }

    public ExtractedResearchChange(
            String description,
            String previousApproach,
            String newApproach,
            String reason,
            Long source,
            Double confidence) {
        this.description = description;
        this.previousApproach = previousApproach;
        this.newApproach = newApproach;
        this.reason = reason;
        this.source = source;
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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