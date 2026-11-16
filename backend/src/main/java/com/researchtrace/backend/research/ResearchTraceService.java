package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.DecisionLineageView;
import com.researchtrace.backend.dto.research.DecisionLineageView.ExperimentEvidence;
import com.researchtrace.backend.dto.research.ExperimentRequest;
import com.researchtrace.backend.dto.research.ResearchDecisionRequest;
import com.researchtrace.backend.dto.research.ResearchTraceSummary;
import com.researchtrace.backend.dto.research.ResultRequest;
import com.researchtrace.backend.dto.research.SupersedeDecisionRequest;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ExperimentStatus;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.model.TextLimits;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ResearchTraceService {

    private final ProjectRepository projectRepository;
    private final MeetingRepository meetingRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ResearchChangeRepository researchChangeRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public ResearchTraceService(
            ProjectRepository projectRepository,
            MeetingRepository meetingRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository,
            ResearchChangeRepository researchChangeRepository,
            TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.meetingRepository = meetingRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
        this.researchChangeRepository = researchChangeRepository;
        this.taskRepository = taskRepository;
    }

    public ResearchTraceService(
            ProjectRepository projectRepository,
            MeetingRepository meetingRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository,
            ResearchChangeRepository researchChangeRepository) {
        this(projectRepository, meetingRepository, researchDecisionRepository, experimentRepository,
             resultRepository, researchEventRepository, researchChangeRepository, null);
    }



    public void requireProject(Long projectId) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
    }

    private Meeting validateMeetingBelongsToProject(Long meetingId, Long projectId) {
        if (meetingId == null) {
            return null;
        }
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + meetingId));
        if (!Objects.equals(meeting.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + meetingId);
        }
        return meeting;
    }

    // -------------------------------------------------------------------------
    // Decisions
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ResearchDecision> getDecisions(Long projectId, ResearchDecisionStatus status) {
        requireProject(projectId);
        if (status != null) {
            return researchDecisionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status);
        }
        return researchDecisionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public ResearchDecision getDecision(Long projectId, Long decisionId) {
        requireProject(projectId);
        ResearchDecision decision = researchDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Decision not found: " + decisionId));
        if (!Objects.equals(decision.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Decision not found: " + decisionId);
        }
        return decision;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForDecision(Long projectId, Long decisionId) {
        ResearchDecision decision = getDecision(projectId, decisionId);
        if (taskRepository == null) {
            return List.of();
        }
        return taskRepository.findByProjectIdAndResearchDecisionId(projectId, decision.getId());
    }

    @Transactional
    public ResearchDecision createDecision(Long projectId, ResearchDecisionRequest request) {
        requireProject(projectId);
        validateMeetingBelongsToProject(request.getMeetingId(), projectId);

        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(projectId);
        decision.setMeetingId(request.getMeetingId());
        decision.setTitle(request.getTitle());
        decision.setDecisionText(request.getDecisionText());
        decision.setRationale(request.getRationale());
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        decision.setSourceReference(request.getSourceReference());
        LocalDate decidedAt = request.getDecidedAt() != null ? request.getDecidedAt() : LocalDate.now();
        decision.setDecidedAt(decidedAt);
        decision.setCreatedAt(LocalDateTime.now());
        decision.setCreatedByUserId(request.getCreatedByUserId());

        ResearchDecision saved = researchDecisionRepository.save(decision);

        // Record in chronological trace
        ResearchEvent event = new ResearchEvent();
        event.setProjectId(projectId);
        event.setEventType(ResearchEventType.DECISION);
        event.setTitle(TextLimits.title("Decision: " + saved.getTitle()));
        event.setDescription(saved.getRationale() != null ? saved.getRationale() : saved.getDecisionText());
        event.setOccurredAt(decidedAt.atStartOfDay());
        event.setCreatedAt(LocalDateTime.now());
        event.setResearchDecisionId(saved.getId());
        event.setMeetingId(saved.getMeetingId());
        event.setSourceReference(saved.getSourceReference());
        event.setCreatedByUserId(saved.getCreatedByUserId());
        researchEventRepository.save(event);

        return saved;
    }

    @Transactional
    public ResearchDecision supersedeDecision(Long projectId, Long oldDecisionId, SupersedeDecisionRequest request) {
        requireProject(projectId);
        ResearchDecision oldDecision = getDecision(projectId, oldDecisionId);

        if (oldDecision.getStatus() == ResearchDecisionStatus.SUPERSEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Decision " + oldDecisionId + " is already superseded.");
        }

        validateMeetingBelongsToProject(request.getMeetingId(), projectId);

        // Mark previous decision as SUPERSEDED (non-destructive preservation)
        oldDecision.setStatus(ResearchDecisionStatus.SUPERSEDED);
        researchDecisionRepository.save(oldDecision);

        // Create new decision pointing to the old one
        ResearchDecision newDecision = new ResearchDecision();
        newDecision.setProjectId(projectId);
        newDecision.setMeetingId(request.getMeetingId());
        newDecision.setTitle(request.getTitle());
        newDecision.setDecisionText(request.getDecisionText());
        newDecision.setRationale(request.getRationale());
        newDecision.setStatus(ResearchDecisionStatus.ACTIVE);
        newDecision.setSupersedesDecisionId(oldDecision.getId());
        newDecision.setSourceReference(request.getSourceReference());
        LocalDate decidedAt = request.getDecidedAt() != null ? request.getDecidedAt() : LocalDate.now();
        newDecision.setDecidedAt(decidedAt);
        newDecision.setCreatedAt(LocalDateTime.now());
        newDecision.setCreatedByUserId(request.getCreatedByUserId());

        ResearchDecision savedNew = researchDecisionRepository.save(newDecision);

        // Record CHANGE event in chronological trace
        ResearchEvent changeEvent = new ResearchEvent();
        changeEvent.setProjectId(projectId);
        changeEvent.setEventType(ResearchEventType.CHANGE);
        changeEvent.setTitle(TextLimits.title("Decision Superseded: " + savedNew.getTitle()));
        changeEvent.setDescription("Superseded \"" + oldDecision.getTitle() + "\". Rationale: " + savedNew.getRationale());
        changeEvent.setOccurredAt(decidedAt.atStartOfDay());
        changeEvent.setCreatedAt(LocalDateTime.now());
        changeEvent.setResearchDecisionId(savedNew.getId());
        changeEvent.setMeetingId(savedNew.getMeetingId());
        changeEvent.setSourceReference(savedNew.getSourceReference());
        changeEvent.setCreatedByUserId(savedNew.getCreatedByUserId());
        researchEventRepository.save(changeEvent);

        return savedNew;
    }

    @Transactional(readOnly = true)
    public DecisionLineageView getDecisionLineage(Long projectId, Long decisionId) {
        ResearchDecision decision = getDecision(projectId, decisionId);

        DecisionLineageView view = new DecisionLineageView();
        view.setDecision(decision);

        // Predecessor if this decision superseded an earlier one
        if (decision.getSupersedesDecisionId() != null) {
            researchDecisionRepository.findById(decision.getSupersedesDecisionId())
                .ifPresent(view::setPredecessorDecision);
        }

        // Successor if another decision superseded this one
        researchDecisionRepository.findBySupersedesDecisionId(decision.getId())
            .ifPresent(view::setSuccessorDecision);

        // Source meeting if attached
        if (decision.getMeetingId() != null) {
            meetingRepository.findById(decision.getMeetingId()).ifPresent(view::setSourceMeeting);
        }

        // Supporting experiments and their results
        List<Experiment> experiments = experimentRepository.findByResearchDecisionId(decision.getId());
        List<ExperimentEvidence> evidenceList = new ArrayList<>();
        for (Experiment exp : experiments) {
            List<Result> results = resultRepository.findByExperimentId(exp.getId());
            evidenceList.add(new ExperimentEvidence(exp, results));
        }
        view.setExperiments(evidenceList);

        return view;
    }

    // -------------------------------------------------------------------------
    // Experiments
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Experiment> getExperiments(Long projectId) {
        requireProject(projectId);
        return experimentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Experiment getExperiment(Long projectId, Long experimentId) {
        requireProject(projectId);
        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found: " + experimentId));
        if (!Objects.equals(experiment.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found: " + experimentId);
        }
        return experiment;
    }

    @Transactional
    public Experiment createExperiment(Long projectId, ExperimentRequest request) {
        requireProject(projectId);

        if (request.getResearchDecisionId() != null) {
            ResearchDecision decision = researchDecisionRepository.findById(request.getResearchDecisionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Guiding decision not found: " + request.getResearchDecisionId()));
            if (!Objects.equals(decision.getProjectId(), projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Guiding decision not found: " + request.getResearchDecisionId());
            }
        }

        validateMeetingBelongsToProject(request.getMeetingId(), projectId);

        Experiment exp = new Experiment();
        exp.setProjectId(projectId);
        exp.setResearchDecisionId(request.getResearchDecisionId());
        exp.setMeetingId(request.getMeetingId());
        exp.setTitle(request.getTitle());
        exp.setHypothesis(request.getHypothesis());
        exp.setMethod(request.getMethod());
        exp.setStatus(request.getStatus() != null ? request.getStatus() : ExperimentStatus.PLANNED);
        exp.setSourceReference(request.getSourceReference());
        exp.setStartedAt(request.getStartedAt());
        exp.setCompletedAt(request.getCompletedAt());
        exp.setCreatedAt(LocalDateTime.now());
        exp.setCreatedByUserId(request.getCreatedByUserId());

        Experiment saved = experimentRepository.save(exp);

        // Record in chronological trace
        ResearchEvent event = new ResearchEvent();
        event.setProjectId(projectId);
        event.setEventType(ResearchEventType.EXPERIMENT);
        event.setTitle(TextLimits.title("Experiment: " + saved.getTitle()));
        event.setDescription(saved.getHypothesis() != null ? saved.getHypothesis() : saved.getMethod());
        LocalDate date = saved.getStartedAt() != null ? saved.getStartedAt() : LocalDate.now();
        event.setOccurredAt(date.atStartOfDay());
        event.setCreatedAt(LocalDateTime.now());
        event.setExperimentId(saved.getId());
        event.setResearchDecisionId(saved.getResearchDecisionId());
        event.setMeetingId(saved.getMeetingId());
        event.setSourceReference(saved.getSourceReference());
        event.setCreatedByUserId(saved.getCreatedByUserId());
        researchEventRepository.save(event);

        return saved;
    }

    @Transactional
    public Experiment updateExperimentDecision(Long projectId, Long experimentId, Long researchDecisionId) {
        Experiment experiment = getExperiment(projectId, experimentId);
        if (researchDecisionId != null) {
            ResearchDecision decision = researchDecisionRepository.findById(researchDecisionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Guiding decision not found: " + researchDecisionId));
            if (!Objects.equals(decision.getProjectId(), projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Decision not found: " + researchDecisionId);
            }
        }
        experiment.setResearchDecisionId(researchDecisionId);
        return experimentRepository.save(experiment);
    }

    // -------------------------------------------------------------------------
    // Results
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Result> getResults(Long projectId) {
        requireProject(projectId);
        return resultRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<Result> getResultsForExperiment(Long projectId, Long experimentId) {
        getExperiment(projectId, experimentId);
        return resultRepository.findByExperimentId(experimentId);
    }

    @Transactional
    public Result recordResult(Long projectId, ResultRequest request) {
        requireProject(projectId);
        Experiment experiment = experimentRepository.findById(request.getExperimentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Experiment not found: " + request.getExperimentId()));
        if (!Objects.equals(experiment.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Experiment not found: " + request.getExperimentId());
        }

        validateMeetingBelongsToProject(request.getMeetingId(), projectId);

        Result res = new Result();
        res.setProjectId(projectId);
        res.setExperimentId(experiment.getId());
        res.setMeetingId(request.getMeetingId() != null ? request.getMeetingId() : experiment.getMeetingId());
        res.setSummary(request.getSummary());
        res.setDataOrMetric(request.getDataOrMetric());
        res.setComparison(request.getComparison());
        res.setSourceReference(request.getSourceReference());
        LocalDate observedAt = request.getObservedAt() != null ? request.getObservedAt() : LocalDate.now();
        res.setObservedAt(observedAt);
        res.setCreatedAt(LocalDateTime.now());
        res.setCreatedByUserId(request.getCreatedByUserId());

        Result saved = resultRepository.save(res);

        // Record in chronological trace
        ResearchEvent event = new ResearchEvent();
        event.setProjectId(projectId);
        event.setEventType(ResearchEventType.RESULT);
        event.setTitle(TextLimits.title("Result: " + experiment.getTitle()));
        event.setDescription(saved.getSummary() + (saved.getDataOrMetric() != null ? " (" + saved.getDataOrMetric() + ")" : ""));
        event.setOccurredAt(observedAt.atStartOfDay());
        event.setCreatedAt(LocalDateTime.now());
        event.setResultId(saved.getId());
        event.setExperimentId(experiment.getId());
        event.setResearchDecisionId(experiment.getResearchDecisionId());
        event.setMeetingId(saved.getMeetingId());
        event.setSourceReference(saved.getSourceReference());
        event.setCreatedByUserId(saved.getCreatedByUserId());
        researchEventRepository.save(event);

        return saved;
    }

    // -------------------------------------------------------------------------
    // Timeline & Changes
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ResearchEvent> getTimeline(Long projectId) {
        requireProject(projectId);
        return researchEventRepository.findByProjectIdOrderByOccurredAtAscCreatedAtAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<ResearchChange> getChanges(Long projectId, ResearchChangeStatus status) {
        requireProject(projectId);
        if (status != null) {
            return researchChangeRepository.findByProjectIdAndStatusOrderByDetectedAtDesc(projectId, status);
        }
        return researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(projectId);
    }

    // -------------------------------------------------------------------------
    // Research Summary for Project
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResearchTraceSummary getSummary(Long projectId) {
        requireProject(projectId);
        List<ResearchDecision> decisions = researchDecisionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Experiment> experiments = experimentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Result> results = resultRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<ResearchEvent> timeline = researchEventRepository.findByProjectIdOrderByOccurredAtAscCreatedAtAsc(projectId);
        List<ResearchChange> changes = researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(projectId);

        ResearchTraceSummary summary = new ResearchTraceSummary();
        summary.setTotalDecisions(decisions.size());
        summary.setActiveDecisions(decisions.stream().filter(d -> d.getStatus() == ResearchDecisionStatus.ACTIVE).count());
        summary.setTotalExperiments(experiments.size());
        summary.setCompletedExperiments(experiments.stream().filter(e -> e.getStatus() == ExperimentStatus.COMPLETED).count());
        summary.setTotalResults(results.size());
        summary.setFlaggedChanges(changes.stream().filter(c -> c.getStatus() == ResearchChangeStatus.FLAGGED).count());

        summary.setDecisions(decisions);
        summary.setExperiments(experiments);
        summary.setResults(results);
        summary.setTimeline(timeline);
        summary.setChanges(changes);

        return summary;
    }
}
