package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.DecisionSupersession;
import com.researchtrace.backend.dto.SupersedeDecisionRequest;
import com.researchtrace.backend.service.ResearchDecisionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/research-decisions")
public class ResearchDecisionController {

    private final ResearchDecisionService researchDecisionService;

    public ResearchDecisionController(ResearchDecisionService researchDecisionService) {
        this.researchDecisionService = researchDecisionService;
    }

    @PostMapping("/{decisionId}/supersede")
    public DecisionSupersession supersede(
            @PathVariable Long projectId,
            @PathVariable Long decisionId,
            @RequestBody(required = false) SupersedeDecisionRequest request) {
        Long replacementDecisionId = request == null ? null : request.replacementDecisionId();
        return researchDecisionService.supersede(projectId, decisionId, replacementDecisionId);
    }
}
