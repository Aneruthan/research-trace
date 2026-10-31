package com.researchtrace.backend.dto.research;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class SupersedeDecisionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String decisionText;

    @NotBlank(message = "Rationale explaining why the decision is superseded is required")
    private String rationale;

    private Long meetingId;

    private String sourceReference;

    private LocalDate decidedAt;

    private Long createdByUserId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDecisionText() {
        return decisionText;
    }

    public void setDecisionText(String decisionText) {
        this.decisionText = decisionText;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public LocalDate getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDate decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
