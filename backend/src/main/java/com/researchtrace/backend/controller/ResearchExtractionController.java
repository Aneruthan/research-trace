package com.researchtrace.backend.controller;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.research.ResearchExtractionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class ResearchExtractionController {

    private final ResearchExtractionService researchExtractionService;

    public ResearchExtractionController(ResearchExtractionService researchExtractionService) {
        this.researchExtractionService = researchExtractionService;
    }

    @PostMapping("/{meetingId}/research-extraction")
    public MeetingExtractionResponse extractResearch(@PathVariable Long meetingId) {
        return researchExtractionService.extract(meetingId);
    }
}
