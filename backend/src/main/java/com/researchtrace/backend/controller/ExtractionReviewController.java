package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.research.ExtractionReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class ExtractionReviewController {

    private final ExtractionReviewService extractionReviewService;

    public ExtractionReviewController(ExtractionReviewService extractionReviewService) {
        this.extractionReviewService = extractionReviewService;
    }

    @PostMapping("/{meetingId}/review")
    public ExtractionReview reviewExtraction(
            @PathVariable Long meetingId,
            @Valid @RequestBody ExtractionReviewRequest reviewRequest) {
        return extractionReviewService.reviewExtraction(meetingId, reviewRequest);
    }

    @GetMapping("/{meetingId}/reviews")
    public List<ExtractionReview> getReviewsForMeeting(@PathVariable Long meetingId) {
        return extractionReviewService.getReviewsForMeeting(meetingId);
    }
}
