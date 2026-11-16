package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.DeletionImpact;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.research.ExtractionReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/meetings")
public class ExtractionReviewController {

    private final ExtractionReviewService extractionReviewService;

    public ExtractionReviewController(ExtractionReviewService extractionReviewService) {
        this.extractionReviewService = extractionReviewService;
    }

    @PostMapping("/{meetingId}/review")
    public ExtractionReview reviewExtraction(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @Valid @RequestBody ExtractionReviewRequest reviewRequest) {
        return extractionReviewService.reviewExtraction(projectId, meetingId, reviewRequest);
    }

    @GetMapping("/{meetingId}/reviews")
    public List<ExtractionReview> getReviewsForMeeting(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return extractionReviewService.getReviewsForMeeting(projectId, meetingId);
    }

    @GetMapping("/{meetingId}/reviews/{reviewId}/deletion-impact")
    public DeletionImpact reviewDeletionImpact(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @PathVariable Long reviewId) {
        return extractionReviewService.deletionImpact(projectId, meetingId, reviewId);
    }

    @DeleteMapping("/{meetingId}/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @PathVariable Long reviewId) {
        extractionReviewService.deleteReview(projectId, meetingId, reviewId);
    }
}
