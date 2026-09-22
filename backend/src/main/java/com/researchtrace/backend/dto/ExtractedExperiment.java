package com.researchtrace.backend.dto;

public class ExtractedExperiment {

    private String title;
    private String description;
    private String objective;
    private String variables;
    private String expectedOutcome;
    private Long source;
    private Double confidence;

    public ExtractedExperiment() {
    }

    public ExtractedExperiment(
            String title,
            String description,
            String objective,
            String variables,
            String expectedOutcome,
            Long source,
            Double confidence) {
        this.title = title;
        this.description = description;
        this.objective = objective;
        this.variables = variables;
        this.expectedOutcome = expectedOutcome;
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

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
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