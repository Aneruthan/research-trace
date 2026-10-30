package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExtractionReviewService {

    private final ExtractionReviewRepository extractionReviewRepository;
    private final MeetingService meetingService;
    private final JsonMapper jsonMapper;

    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository,
            MeetingService meetingService,
            JsonMapper jsonMapper) {
        this.extractionReviewRepository = extractionReviewRepository;
        this.meetingService = meetingService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public ExtractionReview reviewExtraction(Long meetingId, ExtractionReviewRequest reviewRequest) {
        requireMeeting(meetingId);
        if (reviewRequest == null || reviewRequest.getStatus() == null || reviewRequest.getExtraction() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review status and extraction are required");
        }

        ExtractionReview review = new ExtractionReview();
        review.setMeetingId(meetingId);
        review.setStatus(reviewRequest.getStatus());
        review.setExtractionJson(writeExtraction(reviewRequest));
        review.setCreatedAt(LocalDateTime.now());
        return extractionReviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ExtractionReview> getReviewsForMeeting(Long meetingId) {
        requireMeeting(meetingId);
        return extractionReviewRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    private void requireMeeting(Long meetingId) {
        meetingService.getMeetingById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    private String writeExtraction(ExtractionReviewRequest reviewRequest) {
        try {
            return jsonMapper.writeValueAsString(reviewRequest.getExtraction());
        } catch (JacksonException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to store extraction data", exception);
        }
    }
}
