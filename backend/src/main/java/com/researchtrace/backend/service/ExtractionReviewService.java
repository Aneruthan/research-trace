package com.researchtrace.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchtrace.backend.dto.ExtractionReviewRequest;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class ExtractionReviewService {

    private final ExtractionReviewRepository extractionReviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository) {
        this.objectMapper.findAndRegisterModules();
        this.extractionReviewRepository = extractionReviewRepository;
    }

    public ExtractionReview reviewExtraction(
            Long meetingId,
            ExtractionReviewRequest reviewRequest) {

        try {
            ExtractionReview review = new ExtractionReview();

            review.setMeetingId(meetingId);
            review.setStatus(reviewRequest.getStatus());
            review.setExtractionJson(
                    objectMapper.writeValueAsString(
                            reviewRequest.getExtraction()));

            return extractionReviewRepository.save(review);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Unable to store extraction data", e);
        }
    }
    public java.util.List<ExtractionReview> getReviewsForMeeting(
        Long meetingId) {

    return extractionReviewRepository.findByMeetingId(meetingId);
    }
}