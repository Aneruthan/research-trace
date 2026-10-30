package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResearchQueryService {

    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ResearchEventRepository researchEventRepository;

    public ResearchQueryService(
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResultRepository resultRepository,
            ResearchEventRepository researchEventRepository) {
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.researchEventRepository = researchEventRepository;
    }

    public List<ResearchDecision> getResearchDecisions(Long projectId) {
        return researchDecisionRepository.findByProjectIdOrderByDecidedAtAsc(projectId);
    }

    public List<Experiment> getExperiments(Long projectId) {
        return experimentRepository.findByProjectId(projectId);
    }

    public List<Result> getResults(Long projectId) {
        return resultRepository.findByProjectId(projectId);
    }

    public List<ResearchEvent> getResearchEvents(Long projectId) {
        return researchEventRepository.findByProjectIdOrderByOccurredAtAscIdAsc(projectId);
    }
}
