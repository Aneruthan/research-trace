package com.researchtrace.backend.dto;

import com.researchtrace.backend.model.ExtractionReviewStatus;

public class ExtractionReviewRequest {

    private ExtractionReviewStatus status;
    private MeetingExtractionResponse extraction;

    public ExtractionReviewRequest() {
    }

    public ExtractionReviewRequest(
            ExtractionReviewStatus status,
            MeetingExtractionResponse extraction) {
        this.status = status;
        this.extraction = extraction;
    }

    public ExtractionReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ExtractionReviewStatus status) {
        this.status = status;
    }

    public MeetingExtractionResponse getExtraction() {
        return extraction;
    }

    public void setExtraction(MeetingExtractionResponse extraction) {
        this.extraction = extraction;
    }
}