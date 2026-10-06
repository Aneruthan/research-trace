package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.ExtractionReviewRequest;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.service.ExtractionReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
public class ExtractionReviewController {

    private final ExtractionReviewService extractionReviewService;

    public ExtractionReviewController(
            ExtractionReviewService extractionReviewService) {
        this.extractionReviewService = extractionReviewService;
    }

    @PostMapping("/{meetingId}/review")
    public ExtractionReview reviewExtraction(
            @PathVariable Long meetingId,
            @RequestBody ExtractionReviewRequest reviewRequest) {

        return extractionReviewService.reviewExtraction(
                meetingId,
                reviewRequest);
    }
    
    @GetMapping("/{meetingId}/reviews")
    public java.util.List<ExtractionReview> getReviewsForMeeting(
        @PathVariable Long meetingId) {

    return extractionReviewService.getReviewsForMeeting(meetingId);
    }
}