package com.researchtrace.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.researchtrace.backend.model.MilestoneStatus;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A milestone edit. Weight is changed only when the JSON includes {@code weight}.
 * An explicit null clears the stored weight. Omitting the property leaves it unchanged.
 */
public class MilestoneUpdateRequest {

    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;
    private String description;
    private MilestoneStatus status;
    private LocalDate dueDate;
    private Long projectId;
    private BigDecimal weight;

    @JsonIgnore
    private boolean weightPresent;

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

    public MilestoneStatus getStatus() {
        return status;
    }

    public void setStatus(MilestoneStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    @JsonSetter("weight")
    public void setWeight(BigDecimal weight) {
        this.weightPresent = true;
        this.weight = weight;
    }

    public boolean hasWeight() {
        return weightPresent;
    }
}
