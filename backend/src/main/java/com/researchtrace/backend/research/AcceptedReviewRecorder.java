package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.ResearchReviewClaim;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.TextLimits;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResearchReviewClaimRepository;
import com.researchtrace.backend.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AcceptedReviewRecorder {

    private final ResearchReviewClaimRepository researchReviewClaimRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;

    public AcceptedReviewRecorder(
            ResearchReviewClaimRepository researchReviewClaimRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository) {
        this.researchReviewClaimRepository = researchReviewClaimRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
    }

    public void record(ExtractionReview review, Long projectId, MeetingExtractionResponse extraction) {
        String contentHash = sha256(review.getExtractionJson());
        if (researchReviewClaimRepository.existsByMeetingIdAndContentHash(review.getMeetingId(), contentHash)) {
            return;
        }
        ResearchReviewClaim claim = new ResearchReviewClaim();
        claim.setMeetingId(review.getMeetingId());
        claim.setExtractionReviewId(review.getId());
        claim.setContentHash(contentHash);
        researchReviewClaimRepository.saveAndFlush(claim);

        String sourceReference = "meeting:" + review.getMeetingId() + ";review:" + review.getId();
        Map<String, Long> experimentsByTitle = saveExperiments(review, projectId, extraction, sourceReference);
        saveDecisions(review, projectId, extraction, sourceReference);
        saveResults(review, projectId, extraction, sourceReference, experimentsByTitle);
    }

    private void saveDecisions(
            ExtractionReview review,
            Long projectId,
            MeetingExtractionResponse extraction,
            String sourceReference) {
        for (ExtractedDecision extracted : list(extraction.getDecisions())) {
            String title = blankToNull(extracted.getTitle());
            if (title == null) {
                continue;
            }
            ResearchDecision decision = new ResearchDecision();
            decision.setProjectId(projectId);
            if (review.getMeetingId() != null) {
                decision.setMeetingId(review.getMeetingId());
            }
            decision.setExtractionReviewId(review.getId());
            decision.setTitle(TextLimits.title(title));
            decision.setDecisionText(blankToNull(extracted.getDescription()));
            decision.setRationale(blankToNull(extracted.getReason()));
            decision.setStatus(ResearchDecisionStatus.ACTIVE);
            decision.setDecidedAt(review.getCreatedAt());
            decision.setSourceReference(sourceReference);
            ResearchDecision saved = researchDecisionRepository.save(decision);
            saveEvent(review, projectId, ResearchEventType.DECISION, title,
                saved.getDecisionText(), null, saved.getId(), null, null, sourceReference);
        }
    }

    private Map<String, Long> saveExperiments(
            ExtractionReview review,
            Long projectId,
            MeetingExtractionResponse extraction,
            String sourceReference) {
        Map<String, Long> experimentsByTitle = new LinkedHashMap<>();
        for (ExtractedExperiment extracted : list(extraction.getExperiments())) {
            String title = blankToNull(extracted.getTitle());
            if (title == null || experimentsByTitle.containsKey(title)) {
                continue;
            }
            Experiment experiment = new Experiment();
            experiment.setProjectId(projectId);
            if (review.getMeetingId() != null) {
                experiment.setMeetingId(review.getMeetingId());
            }
            experiment.setTitle(TextLimits.title(title));
            experiment.setHypothesis(blankToNull(extracted.getObjective()));
            experiment.setMethod(blankToNull(extracted.getMethod()));
            experiment.setStatus(ExperimentStatus.PLANNED);
            experiment.setSourceReference(sourceReference);
            Experiment saved = experimentRepository.save(experiment);
            experimentsByTitle.put(title, saved.getId());
            saveEvent(review, projectId, ResearchEventType.EXPERIMENT, title,
                saved.getHypothesis(), null, null, saved.getId(), null, sourceReference);
        }
        return experimentsByTitle;
    }

    private void saveResults(
            ExtractionReview review,
            Long projectId,
            MeetingExtractionResponse extraction,
            String sourceReference,
            Map<String, Long> experimentsByTitle) {
        for (ExtractedResult extracted : list(extraction.getResults())) {
            String summary = blankToNull(extracted.getDescription());
            String experimentTitle = blankToNull(extracted.getExperiment());
            if (summary == null || experimentTitle == null) {
                continue;
            }
            Long experimentId = experimentsByTitle.get(experimentTitle);
            if (experimentId == null) {
                continue;
            }
            Result result = new Result();
            result.setProjectId(projectId);
            if (review.getMeetingId() != null) {
                result.setMeetingId(review.getMeetingId());
            }
            result.setExperimentId(experimentId);
            result.setSummary(summary);
            result.setDataOrMetric(blankToNull(extracted.getMetrics()));
            result.setComparison(blankToNull(extracted.getComparison()));
            result.setObservedAt(review.getCreatedAt());
            result.setSourceReference(sourceReference);
            Result saved = resultRepository.save(result);
            saveEvent(review, projectId, ResearchEventType.RESULT, summary,
                saved.getDataOrMetric(), null, null, experimentId, saved.getId(), sourceReference);
        }
    }

    private void saveEvent(
            ExtractionReview review,
            Long projectId,
            ResearchEventType eventType,
            String title,
            String description,
            Long researchDecisionId,
            Long decisionId,
            Long experimentId,
            Long resultId,
            String sourceReference) {
        ResearchEvent event = new ResearchEvent();
        event.setProjectId(projectId);
        event.setEventType(eventType);
        event.setOccurredAt(review.getCreatedAt());
        event.setTitle(TextLimits.title(title));
        event.setDescription(description);
        event.setMeetingId(review.getMeetingId());
        event.setResearchDecisionId(decisionId == null ? researchDecisionId : decisionId);
        event.setExperimentId(experimentId);
        event.setResultId(resultId);
        event.setSourceReference(sourceReference);
        researchEventRepository.save(event);
    }

    private static <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
