package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
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
    private final AcceptedReviewRecorder acceptedReviewRecorder;
    private final ChangeDetectionService changeDetectionService;

    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository,
            MeetingService meetingService,
            JsonMapper jsonMapper,
            AcceptedReviewRecorder acceptedReviewRecorder,
            @org.springframework.lang.Nullable ChangeDetectionService changeDetectionService) {
        this.extractionReviewRepository = extractionReviewRepository;
        this.meetingService = meetingService;
        this.jsonMapper = jsonMapper;
        this.acceptedReviewRecorder = acceptedReviewRecorder;
        this.changeDetectionService = changeDetectionService;
    }

    @Transactional
    public ExtractionReview reviewExtraction(Long projectId, Long meetingId, ExtractionReviewRequest reviewRequest) {
        meetingService.getMeetingForProject(projectId, meetingId);
        return reviewExtraction(meetingId, reviewRequest);
    }

    @Transactional
    public ExtractionReview reviewExtraction(Long meetingId, ExtractionReviewRequest reviewRequest) {
        Meeting meeting = requireMeeting(meetingId);
        if (reviewRequest == null || reviewRequest.getStatus() == null || reviewRequest.getExtraction() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review status and extraction are required");
        }
        if (reviewRequest.getStatus() == ExtractionReviewStatus.ACCEPTED && meeting.getProjectId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting is not linked to a project");
        }
        if (reviewRequest.getStatus() == ExtractionReviewStatus.EDITED) {
            validateExtraction(reviewRequest.getExtraction());
        }

        ExtractionReview review = new ExtractionReview();
        review.setMeetingId(meetingId);
        review.setStatus(reviewRequest.getStatus());
        review.setExtractionJson(writeExtraction(reviewRequest.getExtraction()));
        if (reviewRequest.getOriginalExtraction() != null) {
            review.setOriginalExtractionJson(writeExtraction(reviewRequest.getOriginalExtraction()));
        }
        review.setCreatedAt(LocalDateTime.now());
        ExtractionReview saved = extractionReviewRepository.saveAndFlush(review);

        if (saved.getStatus() == ExtractionReviewStatus.ACCEPTED) {
            acceptedReviewRecorder.record(saved, meeting.getProjectId(), reviewRequest.getExtraction());
            if (changeDetectionService != null && meeting.getProjectId() != null) {
                changeDetectionService.detectAndFlagChanges(meeting.getProjectId(), meetingId, reviewRequest.getExtraction());
            }
        } else if (saved.getStatus() == ExtractionReviewStatus.EDITED) {
            if (changeDetectionService != null && meeting.getProjectId() != null) {
                changeDetectionService.detectAndFlagChanges(meeting.getProjectId(), meetingId, reviewRequest.getExtraction());
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ExtractionReview> getReviewsForMeeting(Long projectId, Long meetingId) {
        meetingService.getMeetingForProject(projectId, meetingId);
        return getReviewsForMeeting(meetingId);
    }

    @Transactional(readOnly = true)
    public List<ExtractionReview> getReviewsForMeeting(Long meetingId) {
        requireMeeting(meetingId);
        return extractionReviewRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    private Meeting requireMeeting(Long meetingId) {
        return meetingService.getMeetingById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    private void validateExtraction(MeetingExtractionResponse extraction) {
        if (extraction.getDecisions() != null) {
            for (ExtractedDecision decision : extraction.getDecisions()) {
                if (decision == null || decision.getTitle() == null || decision.getTitle().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each decision needs a title");
                }
            }
        }
        if (extraction.getExperiments() != null) {
            for (ExtractedExperiment experiment : extraction.getExperiments()) {
                if (experiment == null || experiment.getTitle() == null || experiment.getTitle().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each experiment needs a title");
                }
            }
        }
        if (extraction.getResults() != null) {
            for (ExtractedResult result : extraction.getResults()) {
                if (result == null || result.getDescription() == null || result.getDescription().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each result needs a description");
                }
            }
        }
    }

    private String writeExtraction(MeetingExtractionResponse extraction) {
        try {
            return jsonMapper.writeValueAsString(extraction);
        } catch (JacksonException cause) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not serialize extraction", cause);
        }
    }
}
