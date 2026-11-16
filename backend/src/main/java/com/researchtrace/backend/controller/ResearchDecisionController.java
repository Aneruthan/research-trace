package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.DecisionSupersession;
import com.researchtrace.backend.dto.SupersedeDecisionRequest;
import com.researchtrace.backend.service.ResearchDecisionService;
import org.springframework.http.ResponseEntity;
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

    /**
     * Links two decisions that already exist. The Research Trace page uses
     * {@code POST /api/projects/{projectId}/research/decisions/{decisionId}/supersede},
     * which creates the replacement decision. This endpoint stays because that
     * other contract does not replace it, and backend tests still call it.
     */
    @Deprecated(since = "2026-11-17")
    @PostMapping("/{decisionId}/supersede")
    public ResponseEntity<DecisionSupersession> supersede(
            @PathVariable Long projectId,
            @PathVariable Long decisionId,
            @RequestBody(required = false) SupersedeDecisionRequest request) {
        Long replacementDecisionId = request == null ? null : request.replacementDecisionId();
        DecisionSupersession result = researchDecisionService.supersede(projectId, decisionId, replacementDecisionId);
        return ResponseEntity.ok().header("Deprecation", "true").body(result);
    }
}
