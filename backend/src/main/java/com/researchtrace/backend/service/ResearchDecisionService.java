package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.DecisionSupersession;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
public class ResearchDecisionService {

    private final ResearchDecisionRepository researchDecisionRepository;

    public ResearchDecisionService(ResearchDecisionRepository researchDecisionRepository) {
        this.researchDecisionRepository = researchDecisionRepository;
    }

    @Transactional
    public DecisionSupersession supersede(Long projectId, Long decisionId, Long replacementDecisionId) {
        if (projectId == null || decisionId == null || replacementDecisionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision id and replacement decision id are required");
        }
        if (decisionId.equals(replacementDecisionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A decision cannot supersede itself");
        }

        ResearchDecision previous = requireDecision(decisionId);
        ResearchDecision replacement = requireDecision(replacementDecisionId);
        if (!projectId.equals(previous.getProjectId()) || !projectId.equals(replacement.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Decision not found");
        }
        if (decisionId.equals(replacement.getSupersedesDecisionId())
                && previous.getStatus() == ResearchDecisionStatus.SUPERSEDED) {
            return new DecisionSupersession(previous, replacement);
        }
        rejectCycle(previous, replacement);
        if (previous.getStatus() == ResearchDecisionStatus.SUPERSEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Decision is already superseded");
        }
        if (replacement.getStatus() == ResearchDecisionStatus.SUPERSEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement decision is already superseded");
        }
        if (replacement.getSupersedesDecisionId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement decision already supersedes another decision");
        }
        boolean claimedByAnother = researchDecisionRepository.findBySupersedesDecisionId(previous.getId()).stream()
            .anyMatch(decision -> !decision.getId().equals(replacement.getId()));
        if (claimedByAnother) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Decision is already superseded");
        }

        persist(previous, replacement);
        return new DecisionSupersession(previous, replacement);
    }

    protected void persist(ResearchDecision previous, ResearchDecision replacement) {
        replacement.setSupersedesDecisionId(previous.getId());
        previous.setStatus(ResearchDecisionStatus.SUPERSEDED);
        researchDecisionRepository.save(previous);
        researchDecisionRepository.save(replacement);
    }

    private void rejectCycle(ResearchDecision previous, ResearchDecision replacement) {
        Set<Long> seen = new HashSet<>();
        Long cursor = previous.getSupersedesDecisionId();
        while (cursor != null) {
            if (cursor.equals(replacement.getId()) || cursor.equals(previous.getId()) || !seen.add(cursor)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Supersession would create a cycle");
            }
            ResearchDecision linked = researchDecisionRepository.findById(cursor).orElse(null);
            cursor = linked == null ? null : linked.getSupersedesDecisionId();
        }
    }

    private ResearchDecision requireDecision(Long decisionId) {
        return researchDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Research decision not found"));
    }
}
