package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.DeletionImpact;
import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
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
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ResearchReviewClaimRepository researchReviewClaimRepository;

    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository,
            MeetingService meetingService,
            JsonMapper jsonMapper,
            AcceptedReviewRecorder acceptedReviewRecorder,
            @org.springframework.lang.Nullable ChangeDetectionService changeDetectionService) {
        this(extractionReviewRepository, meetingService, jsonMapper, acceptedReviewRecorder, changeDetectionService,
            null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository,
            MeetingService meetingService,
            JsonMapper jsonMapper,
            AcceptedReviewRecorder acceptedReviewRecorder,
            @org.springframework.lang.Nullable ChangeDetectionService changeDetectionService,
            @org.springframework.lang.Nullable ResearchDecisionRepository researchDecisionRepository,
            @org.springframework.lang.Nullable ExperimentRepository experimentRepository,
            @org.springframework.lang.Nullable ResultRepository resultRepository,
            @org.springframework.lang.Nullable ResearchEventRepository researchEventRepository,
            @org.springframework.lang.Nullable ResearchReviewClaimRepository researchReviewClaimRepository) {
        this.extractionReviewRepository = extractionReviewRepository;
        this.meetingService = meetingService;
        this.jsonMapper = jsonMapper;
        this.acceptedReviewRecorder = acceptedReviewRecorder;
        this.changeDetectionService = changeDetectionService;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
        this.researchReviewClaimRepository = researchReviewClaimRepository;
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
        if (reviewRequest.getStatus() == ExtractionReviewStatus.EDITED
                || reviewRequest.getStatus() == ExtractionReviewStatus.ACCEPTED) {
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

    public DeletionImpact deletionImpact(Long projectId, Long meetingId, Long reviewId) {
        meetingService.getMeetingForProject(projectId, meetingId);
        return impactFor(requireReview(meetingId, reviewId));
    }

    @Transactional
    public void deleteReview(Long projectId, Long meetingId, Long reviewId) {
        meetingService.getMeetingForProject(projectId, meetingId);
        ExtractionReview review = requireReview(meetingId, reviewId);
        DeletionImpact impact = impactFor(review);
        if (impact.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, impact.getMessage());
        }
        if (researchReviewClaimRepository != null) {
            researchReviewClaimRepository.deleteByExtractionReviewId(review.getId());
        }
        extractionReviewRepository.delete(review);
    }

    private ExtractionReview requireReview(Long meetingId, Long reviewId) {
        ExtractionReview review = extractionReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction review not found"));
        if (!meetingId.equals(review.getMeetingId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction review not found");
        }
        return review;
    }

    private DeletionImpact impactFor(ExtractionReview review) {
        int decisions = researchDecisionRepository == null ? 0 : researchDecisionRepository.findByExtractionReviewId(review.getId()).size();
        int experiments = experimentRepository == null ? 0 : experimentRepository.findByExtractionReviewId(review.getId()).size();
        int results = resultRepository == null ? 0 : resultRepository.findByExtractionReviewId(review.getId()).size();
        int events = 0;
        if (researchEventRepository != null) {
            String marker = "review:" + review.getId();
            for (ResearchEvent event : researchEventRepository.findByMeetingId(review.getMeetingId())) {
                if (event.getSourceReference() != null && event.getSourceReference().contains(marker)) {
                    events++;
                }
            }
        }
        DeletionImpact impact = new DeletionImpact();
        impact.setTitle(review.getStatus() == null ? "Extraction review" : review.getStatus().name());
        impact.setResearchDecisions(decisions);
        impact.setExperiments(experiments);
        impact.setResults(results);
        impact.setResearchEvents(events);
        impact.setAcceptedReviews(review.getStatus() == ExtractionReviewStatus.ACCEPTED ? 1 : 0);
        boolean blocked = decisions + experiments + results + events > 0;
        impact.setBlocked(blocked);
        if (blocked) {
            impact.setMessage("Cannot delete this extraction review because accepted research records still point at it. "
                + "Decisions: " + decisions
                + ". Experiments: " + experiments
                + ". Results: " + results
                + ". Research events: " + events
                + ". Those records were kept. Nothing was deleted.");
        } else {
            impact.setMessage("This review has no accepted research records. Only this review will be deleted. The meeting and other reviews stay.");
        }
        return impact;
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
