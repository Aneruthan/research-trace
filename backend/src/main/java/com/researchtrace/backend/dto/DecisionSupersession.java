package com.researchtrace.backend.dto;

import com.researchtrace.backend.model.ResearchDecision;

public record DecisionSupersession(ResearchDecision superseded, ResearchDecision replacement) {
}
