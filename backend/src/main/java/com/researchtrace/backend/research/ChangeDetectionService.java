package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedResearchChange;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchChangeStatus;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ChangeDetectionService {

    private final ProjectRepository projectRepository;
    private final MeetingRepository meetingRepository;
    private final ResearchChangeRepository researchChangeRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ResearchEventRepository researchEventRepository;

    public ChangeDetectionService(
            ProjectRepository projectRepository,
            MeetingRepository meetingRepository,
            ResearchChangeRepository researchChangeRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ResearchEventRepository researchEventRepository) {
        this.projectRepository = projectRepository;
        this.meetingRepository = meetingRepository;
        this.researchChangeRepository = researchChangeRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.researchEventRepository = researchEventRepository;
    }

    @Transactional
    public List<ResearchChange> detectAndFlagChanges(Long projectId, Long meetingId, MeetingExtractionResponse extraction) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }

        Meeting meeting = null;
        if (meetingId != null) {
            meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + meetingId));
            if (!Objects.equals(meeting.getProjectId(), projectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting does not belong to project " + projectId);
            }
        }

        List<ResearchChange> flaggedChanges = new ArrayList<>();
        if (extraction == null) {
            return flaggedChanges;
        }

        List<ResearchChange> existingChanges = researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(projectId);

        // 1. Ingest extracted research changes explicitly detected by AI
        if (extraction.getResearchChanges() != null) {
            for (ExtractedResearchChange changeDto : extraction.getResearchChanges()) {
                if (changeDto.getPreviousApproach() == null && changeDto.getNewApproach() == null && changeDto.getDescription() == null) {
                    continue;
                }

                boolean isDuplicate = existingChanges.stream().anyMatch(existing ->
                    isSimilar(existing.getPreviousApproach(), changeDto.getPreviousApproach())
                    && isSimilar(existing.getNewApproach(), changeDto.getNewApproach())
                );

                if (!isDuplicate) {
                    ResearchChange change = new ResearchChange();
                    change.setProjectId(projectId);
                    change.setMeetingId(meetingId);
                    change.setChangeType(determineChangeType(changeDto.getPreviousApproach(), changeDto.getNewApproach()));
                    change.setTitle(changeDto.getDescription() != null ? changeDto.getDescription() : "Detected Research Change");
                    change.setPreviousApproach(changeDto.getPreviousApproach());
                    change.setNewApproach(changeDto.getNewApproach());
                    change.setReason(changeDto.getReason());
                    change.setSourceReference(meeting != null ? "Meeting: " + meeting.getTitle() : "Meeting ID " + meetingId);
                    change.setStatus(ResearchChangeStatus.FLAGGED);
                    change.setDetectedAt(LocalDateTime.now());
                    change.setCreatedAt(LocalDateTime.now());

                    ResearchChange saved = researchChangeRepository.save(change);
                    flaggedChanges.add(saved);

                    // Publish to chronological trace
                    ResearchEvent event = new ResearchEvent();
                    event.setProjectId(projectId);
                    event.setEventType(ResearchEventType.CHANGE);
                    event.setTitle("Flagged Change: " + saved.getTitle());
                    event.setDescription("Shift from [" + saved.getPreviousApproach() + "] to [" + saved.getNewApproach() + "]. Reason: " + saved.getReason());
                    event.setOccurredAt(LocalDateTime.now());
                    event.setCreatedAt(LocalDateTime.now());
                    event.setMeetingId(meetingId);
                    event.setResearchChangeId(saved.getId());
                    event.setSourceReference(saved.getSourceReference());
                    researchEventRepository.save(event);
                }
            }
        }

        // 2. Cross-meeting comparison against existing active decisions in this project
        List<ResearchDecision> activeDecisions = researchDecisionRepository
            .findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, ResearchDecisionStatus.ACTIVE);

        if (extraction.getDecisions() != null && !activeDecisions.isEmpty()) {
            for (ExtractedDecision candidate : extraction.getDecisions()) {
                String candidateTitle = candidate.getTitle() != null ? candidate.getTitle().toLowerCase() : "";
                String candidateDesc = candidate.getDescription() != null ? candidate.getDescription().toLowerCase() : "";

                for (ResearchDecision active : activeDecisions) {
                    // Check if candidate suggests a pivot from an active decision (e.g. PMC-VQA -> LitMedImage)
                    if (candidateTitle.contains("switch") || candidateTitle.contains("pivot") || candidateTitle.contains("instead of")) {
                        boolean alreadyFlagged = existingChanges.stream().anyMatch(e ->
                            isSimilar(e.getTitle(), candidate.getTitle())
                        );

                        if (!alreadyFlagged && !active.getTitle().equalsIgnoreCase(candidate.getTitle())) {
                            ResearchChange change = new ResearchChange();
                            change.setProjectId(projectId);
                            change.setMeetingId(meetingId);
                            change.setChangeType("DIRECTION");
                            change.setTitle("Potential Direction Shift: " + candidate.getTitle());
                            change.setPreviousApproach(active.getTitle());
                            change.setNewApproach(candidate.getTitle());
                            change.setReason(candidate.getReason() != null ? candidate.getReason() : candidate.getDescription());
                            change.setSourceReference(meeting != null ? "Meeting: " + meeting.getTitle() : "Meeting ID " + meetingId);
                            change.setStatus(ResearchChangeStatus.FLAGGED);
                            change.setDetectedAt(LocalDateTime.now());
                            change.setCreatedAt(LocalDateTime.now());

                            ResearchChange saved = researchChangeRepository.save(change);
                            flaggedChanges.add(saved);
                            break;
                        }
                    }
                }
            }
        }

        return flaggedChanges;
    }

    @Transactional
    public ResearchChange updateChangeStatus(Long projectId, Long changeId, ResearchChangeStatus newStatus) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status cannot be null");
        }

        ResearchChange change = researchChangeRepository.findById(changeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Research change not found: " + changeId));

        if (!Objects.equals(change.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change does not belong to project " + projectId);
        }

        change.setStatus(newStatus);
        return researchChangeRepository.save(change);
    }

    private static String determineChangeType(String prev, String next) {
        String combined = ((prev != null ? prev : "") + " " + (next != null ? next : "")).toLowerCase();
        if (combined.contains("dataset") || combined.contains("data") || combined.contains("corpus")) {
            return "DATASET";
        }
        if (combined.contains("model") || combined.contains("encoder") || combined.contains("bert") || combined.contains("clip")) {
            return "MODEL";
        }
        if (combined.contains("experiment") || combined.contains("baseline") || combined.contains("metric")) {
            return "EXPERIMENT";
        }
        return "DIRECTION";
    }

    private static boolean isSimilar(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
