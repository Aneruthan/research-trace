package com.researchtrace.backend.dto.research;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ResultRequest {

    @NotNull(message = "Experiment ID is required")
    private Long experimentId;

    private Long meetingId;

    @NotBlank(message = "Summary is required")
    private String summary;

    private String dataOrMetric;

    private String comparison;

    private String sourceReference;

    private LocalDate observedAt;

    private Long createdByUserId;

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDataOrMetric() {
        return dataOrMetric;
    }

    public void setDataOrMetric(String dataOrMetric) {
        this.dataOrMetric = dataOrMetric;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public LocalDate getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDate observedAt) {
        this.observedAt = observedAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
