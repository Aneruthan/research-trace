package com.researchtrace.backend.dto;

public class ExtractedResult {

    private String description;
    private String experiment;
    private String metrics;
    private String comparison;
    private Long source;
    private Double confidence;

    public ExtractedResult() {
    }

    public ExtractedResult(
            String description,
            String experiment,
            String metrics,
            String comparison,
            Long source,
            Double confidence) {
        this.description = description;
        this.experiment = experiment;
        this.metrics = metrics;
        this.comparison = comparison;
        this.source = source;
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getMetrics() {
        return metrics;
    }

    public void setMetrics(String metrics) {
        this.metrics = metrics;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
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