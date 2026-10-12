package com.researchtrace.backend.dto.research;

import com.researchtrace.backend.model.ExtractionReviewStatus;
import jakarta.validation.constraints.NotNull;

public class ExtractionReviewRequest {

    @NotNull
    private ExtractionReviewStatus status;

    @NotNull
    private MeetingExtractionResponse extraction;

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
