package com.researchtrace.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Reviewed values for a task suggestion. A field left out of the JSON keeps the suggestion's
 * current value. A field sent as null clears that value. The title stays mandatory when it is sent.
 */
public class AcceptTaskSuggestionRequest {

    public enum ReviewedField {
        TITLE,
        DESCRIPTION,
        ASSIGNEE_ID,
        MILESTONE_ID,
        DUE_DATE,
        RESEARCH_DECISION_ID,
        EXPERIMENT_ID
    }

    @JsonIgnore
    private final EnumSet<ReviewedField> present = EnumSet.noneOf(ReviewedField.class);

    private String title;
    private String description;
    private Long assigneeId;
    private Long milestoneId;
    private LocalDate dueDate;
    private Long researchDecisionId;
    private Long experimentId;

    public AcceptTaskSuggestionRequest() {
    }

    public AcceptTaskSuggestionRequest(
            String title,
            String description,
            Long assigneeId,
            Long milestoneId,
            LocalDate dueDate) {
        this(title, description, assigneeId, milestoneId, dueDate, null, null);
    }

    /**
     * A null argument is treated as omitted, matching callers that only fill the fields they edit.
     * JSON null is different: the setters record the field as present and clear it.
     */
    public AcceptTaskSuggestionRequest(
            String title,
            String description,
            Long assigneeId,
            Long milestoneId,
            LocalDate dueDate,
            Long researchDecisionId,
            Long experimentId) {
        include(ReviewedField.TITLE, title);
        include(ReviewedField.DESCRIPTION, description);
        include(ReviewedField.ASSIGNEE_ID, assigneeId);
        include(ReviewedField.MILESTONE_ID, milestoneId);
        include(ReviewedField.DUE_DATE, dueDate);
        include(ReviewedField.RESEARCH_DECISION_ID, researchDecisionId);
        include(ReviewedField.EXPERIMENT_ID, experimentId);
    }

    public boolean has(ReviewedField field) {
        return present.contains(field);
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Long assigneeId() {
        return assigneeId;
    }

    public Long milestoneId() {
        return milestoneId;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Long researchDecisionId() {
        return researchDecisionId;
    }

    public Long experimentId() {
        return experimentId;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        present.add(ReviewedField.TITLE);
        this.title = title;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        present.add(ReviewedField.DESCRIPTION);
        this.description = description;
    }

    @JsonSetter("assigneeId")
    public void setAssigneeId(Long assigneeId) {
        present.add(ReviewedField.ASSIGNEE_ID);
        this.assigneeId = assigneeId;
    }

    @JsonSetter("milestoneId")
    public void setMilestoneId(Long milestoneId) {
        present.add(ReviewedField.MILESTONE_ID);
        this.milestoneId = milestoneId;
    }

    @JsonSetter("dueDate")
    public void setDueDate(LocalDate dueDate) {
        present.add(ReviewedField.DUE_DATE);
        this.dueDate = dueDate;
    }

    @JsonSetter("researchDecisionId")
    public void setResearchDecisionId(Long researchDecisionId) {
        present.add(ReviewedField.RESEARCH_DECISION_ID);
        this.researchDecisionId = researchDecisionId;
    }

    @JsonSetter("experimentId")
    public void setExperimentId(Long experimentId) {
        present.add(ReviewedField.EXPERIMENT_ID);
        this.experimentId = experimentId;
    }

    private void include(ReviewedField field, Object value) {
        if (value == null) {
            return;
        }
        present.add(field);
        switch (field) {
            case TITLE -> title = (String) value;
            case DESCRIPTION -> description = (String) value;
            case ASSIGNEE_ID -> assigneeId = (Long) value;
            case MILESTONE_ID -> milestoneId = (Long) value;
            case DUE_DATE -> dueDate = (LocalDate) value;
            case RESEARCH_DECISION_ID -> researchDecisionId = (Long) value;
            case EXPERIMENT_ID -> experimentId = (Long) value;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AcceptTaskSuggestionRequest request)) {
            return false;
        }
        return present.equals(request.present)
            && Objects.equals(title, request.title)
            && Objects.equals(description, request.description)
            && Objects.equals(assigneeId, request.assigneeId)
            && Objects.equals(milestoneId, request.milestoneId)
            && Objects.equals(dueDate, request.dueDate)
            && Objects.equals(researchDecisionId, request.researchDecisionId)
            && Objects.equals(experimentId, request.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(present, title, description, assigneeId, milestoneId, dueDate,
            researchDecisionId, experimentId);
    }
}
