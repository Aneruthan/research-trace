package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractionReviewRequest;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
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
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ChangeDetectionService changeDetectionService;

    public ExtractionReviewService(
            ExtractionReviewRepository extractionReviewRepository,
            MeetingService meetingService,
            JsonMapper jsonMapper,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository,
            ChangeDetectionService changeDetectionService) {
        this.extractionReviewRepository = extractionReviewRepository;
        this.meetingService = meetingService;
        this.jsonMapper = jsonMapper;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
        this.changeDetectionService = changeDetectionService;
    }

    @Transactional
    public ExtractionReview reviewExtraction(Long meetingId, ExtractionReviewRequest reviewRequest) {
        Meeting meeting = requireMeeting(meetingId);
        if (reviewRequest == null || reviewRequest.getStatus() == null || reviewRequest.getExtraction() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review status and extraction are required");
        }

        ExtractionReview review = new ExtractionReview();
        review.setMeetingId(meetingId);
        review.setStatus(reviewRequest.getStatus());
        review.setExtractionJson(writeExtraction(reviewRequest));
        review.setCreatedAt(LocalDateTime.now());
        ExtractionReview savedReview = extractionReviewRepository.save(review);

        // Human verification promotion: ACCEPTED or EDITED items are promoted to persistent domain records
        if (reviewRequest.getStatus() == ExtractionReviewStatus.ACCEPTED || reviewRequest.getStatus() == ExtractionReviewStatus.EDITED) {
            ingestReviewedItems(meeting, reviewRequest.getExtraction());
        }

        return savedReview;
    }

    @Transactional(readOnly = true)
    public List<ExtractionReview> getReviewsForMeeting(Long meetingId) {
        requireMeeting(meetingId);
        return extractionReviewRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    private void ingestReviewedItems(Meeting meeting, MeetingExtractionResponse extraction) {
        Long projectId = meeting.getProjectId();
        Long meetingId = meeting.getId();
        String sourceProvenance = "Meeting " + meetingId + (meeting.getTitle() != null ? " (" + meeting.getTitle() + ")" : "");

        // 1. Ingest Decisions
        if (extraction.getDecisions() != null) {
            List<ResearchDecision> existingDecisions = researchDecisionRepository.findByMeetingId(meetingId);
            for (ExtractedDecision candidate : extraction.getDecisions()) {
                if (candidate.getTitle() == null || candidate.getTitle().isBlank()) {
                    continue;
                }
                boolean exists = existingDecisions.stream()
                    .anyMatch(d -> d.getTitle().equalsIgnoreCase(candidate.getTitle().trim()));

                if (!exists) {
                    ResearchDecision decision = new ResearchDecision();
                    decision.setProjectId(projectId);
                    decision.setMeetingId(meetingId);
                    decision.setTitle(candidate.getTitle().trim());
                    decision.setDecisionText(candidate.getDescription());
                    decision.setRationale(candidate.getReason());
                    decision.setStatus(ResearchDecisionStatus.ACTIVE);
                    decision.setSourceReference(sourceProvenance + (candidate.getDescription() != null ? ": " + candidate.getDescription() : ""));
                    decision.setDecidedAt(meeting.getMeetingDate());
                    decision.setCreatedAt(LocalDateTime.now());

                    ResearchDecision saved = researchDecisionRepository.save(decision);
                    if (saved == null) {
                        saved = decision;
                    }

                    ResearchEvent event = new ResearchEvent();
                    event.setProjectId(projectId);
                    event.setEventType(ResearchEventType.DECISION);
                    event.setTitle("Decision: " + saved.getTitle());
                    event.setDescription(saved.getRationale() != null ? saved.getRationale() : saved.getDecisionText());
                    event.setOccurredAt(meeting.getMeetingDate().atStartOfDay());
                    event.setCreatedAt(LocalDateTime.now());
                    event.setMeetingId(meetingId);
                    event.setResearchDecisionId(saved.getId());
                    event.setSourceReference(saved.getSourceReference());
                    researchEventRepository.save(event);
                }
            }
        }

        // 2. Ingest Experiments
        Experiment primaryMeetingExperiment = null;
        if (extraction.getExperiments() != null) {
            List<Experiment> existingExperiments = experimentRepository.findByMeetingId(meetingId);
            for (ExtractedExperiment candidate : extraction.getExperiments()) {
                if (candidate.getTitle() == null || candidate.getTitle().isBlank()) {
                    continue;
                }
                boolean exists = existingExperiments.stream()
                    .anyMatch(e -> e.getTitle().equalsIgnoreCase(candidate.getTitle().trim()));

                if (!exists) {
                    Experiment exp = new Experiment();
                    exp.setProjectId(projectId);
                    exp.setMeetingId(meetingId);
                    exp.setTitle(candidate.getTitle().trim());
                    exp.setHypothesis(candidate.getObjective() != null ? candidate.getObjective() : candidate.getDescription());
                    exp.setMethod(candidate.getVariables() != null ? candidate.getVariables() : candidate.getDescription());
                    exp.setStatus(ExperimentStatus.PLANNED);
                    exp.setSourceReference(sourceProvenance);
                    exp.setStartedAt(meeting.getMeetingDate());
                    exp.setCreatedAt(LocalDateTime.now());

                    Experiment saved = experimentRepository.save(exp);
                    if (saved == null) {
                        saved = exp;
                    }
                    if (primaryMeetingExperiment == null) {
                        primaryMeetingExperiment = saved;
                    }

                    ResearchEvent event = new ResearchEvent();
                    event.setProjectId(projectId);
                    event.setEventType(ResearchEventType.EXPERIMENT);
                    event.setTitle("Experiment: " + saved.getTitle());
                    event.setDescription(saved.getHypothesis());
                    event.setOccurredAt(meeting.getMeetingDate().atStartOfDay());
                    event.setCreatedAt(LocalDateTime.now());
                    event.setMeetingId(meetingId);
                    event.setExperimentId(saved.getId());
                    event.setSourceReference(saved.getSourceReference());
                    researchEventRepository.save(event);
                }
            }
        }

        // 3. Ingest Results
        if (extraction.getResults() != null) {
            List<Result> existingResults = resultRepository.findByMeetingId(meetingId);
            List<Experiment> projectExperiments = experimentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

            for (ExtractedResult candidate : extraction.getResults()) {
                if (candidate.getDescription() == null || candidate.getDescription().isBlank()) {
                    continue;
                }
                boolean exists = existingResults.stream()
                    .anyMatch(r -> r.getSummary().equalsIgnoreCase(candidate.getDescription().trim()));

                if (!exists) {
                    Long targetExperimentId = null;
                    if (candidate.getExperiment() != null) {
                        targetExperimentId = projectExperiments.stream()
                            .filter(e -> e.getTitle().toLowerCase().contains(candidate.getExperiment().toLowerCase()))
                            .map(Experiment::getId)
                            .findFirst()
                            .orElse(null);
                    }
                    if (targetExperimentId == null && primaryMeetingExperiment != null) {
                        targetExperimentId = primaryMeetingExperiment.getId();
                    }
                    if (targetExperimentId == null && !projectExperiments.isEmpty()) {
                        targetExperimentId = projectExperiments.get(0).getId();
                    }

                    if (targetExperimentId != null) {
                        Result result = new Result();
                        result.setProjectId(projectId);
                        result.setMeetingId(meetingId);
                        result.setExperimentId(targetExperimentId);
                        result.setSummary(candidate.getDescription().trim());
                        result.setDataOrMetric(candidate.getMetrics());
                        result.setComparison(candidate.getComparison());
                        result.setSourceReference(sourceProvenance);
                        result.setObservedAt(meeting.getMeetingDate());
                        result.setCreatedAt(LocalDateTime.now());

                        Result saved = resultRepository.save(result);
                        if (saved == null) {
                            saved = result;
                        }

                        ResearchEvent event = new ResearchEvent();
                        event.setProjectId(projectId);
                        event.setEventType(ResearchEventType.RESULT);
                        event.setTitle("Result: " + saved.getSummary());
                        event.setDescription(saved.getDataOrMetric());
                        event.setOccurredAt(meeting.getMeetingDate().atStartOfDay());
                        event.setCreatedAt(LocalDateTime.now());
                        event.setMeetingId(meetingId);
                        event.setResultId(saved.getId());
                        event.setExperimentId(saved.getExperimentId());
                        event.setSourceReference(saved.getSourceReference());
                        researchEventRepository.save(event);
                    }
                }
            }
        }

        // 4. Change Detection across meetings
        changeDetectionService.detectAndFlagChanges(projectId, meetingId, extraction);
    }

    private Meeting requireMeeting(Long meetingId) {
        return meetingService.getMeetingById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + meetingId));
    }

    private String writeExtraction(ExtractionReviewRequest reviewRequest) {
        try {
            return jsonMapper.writeValueAsString(reviewRequest.getExtraction());
        } catch (JacksonException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to store extraction data", exception);
        }
    }
}
