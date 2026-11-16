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
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeDetectionServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long MEETING_ID = 20L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ResearchChangeRepository researchChangeRepository;

    @Mock
    private ResearchDecisionRepository researchDecisionRepository;

    @Mock
    private ResearchEventRepository researchEventRepository;

    private ChangeDetectionService changeDetectionService;

    @BeforeEach
    void setUp() {
        changeDetectionService = new ChangeDetectionService(
            projectRepository,
            meetingRepository,
            researchChangeRepository,
            researchDecisionRepository,
            researchEventRepository
        );
        lenient().when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    }

    @Test
    void detectsAndFlagsExtractedResearchChange() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(PROJECT_ID);
        meeting.setTitle("Evaluation review");
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        ExtractedResearchChange change = new ExtractedResearchChange();
        change.setDescription("Switch from PMC-VQA to LitMedImage");
        change.setPreviousApproach("PMC-VQA dataset");
        change.setNewApproach("LitMedImage dataset");
        change.setReason("Annotation label noise in PMC-VQA");
        extraction.setResearchChanges(List.of(change));

        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID))
            .thenReturn(Collections.emptyList());
        when(researchChangeRepository.save(any(ResearchChange.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(PROJECT_ID, MEETING_ID, extraction);

        assertThat(flagged).hasSize(1);
        ResearchChange savedChange = flagged.get(0);
        assertThat(savedChange.getChangeType()).isEqualTo("DATASET");
        assertThat(savedChange.getPreviousApproach()).isEqualTo("PMC-VQA dataset");
        assertThat(savedChange.getNewApproach()).isEqualTo("LitMedImage dataset");
        assertThat(savedChange.getStatus()).isEqualTo(ResearchChangeStatus.FLAGGED);

        verify(researchEventRepository).save(any(ResearchEvent.class));
    }

    @Test
    void preventsDuplicateChangeDetection() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(PROJECT_ID);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        ResearchChange existing = new ResearchChange();
        existing.setProjectId(PROJECT_ID);
        existing.setPreviousApproach("PMC-VQA dataset");
        existing.setNewApproach("LitMedImage dataset");
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID))
            .thenReturn(List.of(existing));

        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        ExtractedResearchChange change = new ExtractedResearchChange();
        change.setPreviousApproach("PMC-VQA dataset");
        change.setNewApproach("LitMedImage dataset");
        extraction.setResearchChanges(List.of(change));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(PROJECT_ID, MEETING_ID, extraction);

        assertThat(flagged).isEmpty();
        verify(researchChangeRepository, never()).save(any());
    }

    @Test
    void detectsCrossMeetingDirectionShiftAgainstActiveDecisions() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(PROJECT_ID);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        ResearchDecision activeDecision = new ResearchDecision();
        activeDecision.setId(100L);
        activeDecision.setTitle("Use PMC-VQA for primary evaluation");
        activeDecision.setStatus(ResearchDecisionStatus.ACTIVE);

        when(researchDecisionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(PROJECT_ID, ResearchDecisionStatus.ACTIVE))
            .thenReturn(List.of(activeDecision));
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID))
            .thenReturn(Collections.emptyList());
        when(researchChangeRepository.save(any(ResearchChange.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        ExtractedDecision newDecision = new ExtractedDecision();
        newDecision.setTitle("Switch to LitMedImage dataset");
        newDecision.setReason("Clinical validation required");
        extraction.setDecisions(List.of(newDecision));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(PROJECT_ID, MEETING_ID, extraction);

        assertThat(flagged).hasSize(1);
        assertThat(flagged.get(0).getTitle()).contains("Potential Direction Shift");
        assertThat(flagged.get(0).getPreviousApproach()).isEqualTo("Use PMC-VQA for primary evaluation");
        assertThat(flagged.get(0).getNewApproach()).isEqualTo("Switch to LitMedImage dataset");
    }

    @Test
    void doesNotRepeatADirectionShiftThatWasAlreadyFlagged() {
        stubMeeting();
        when(researchDecisionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(PROJECT_ID, ResearchDecisionStatus.ACTIVE))
            .thenReturn(List.of(activeDecision(100L, "Use PMC-VQA for primary evaluation", 5L)));
        ResearchChange existing = new ResearchChange();
        existing.setProjectId(PROJECT_ID);
        existing.setTitle("Potential Direction Shift: Switch to LitMedImage dataset");
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID)).thenReturn(List.of(existing));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(
            PROJECT_ID, MEETING_ID, extractionWithDecision("Switch to LitMedImage dataset"));

        assertThat(flagged).isEmpty();
        verify(researchChangeRepository, never()).save(any());
    }

    @Test
    void decisionsFromTheSameMeetingAreNotTreatedAsThePreviousApproach() {
        stubMeeting();
        when(researchDecisionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(PROJECT_ID, ResearchDecisionStatus.ACTIVE))
            .thenReturn(List.of(activeDecision(101L, "Adopt PMC-VQA benchmark", MEETING_ID)));
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID)).thenReturn(Collections.emptyList());

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(
            PROJECT_ID, MEETING_ID, extractionWithDecision("Switch dataset to LitMedImage"));

        assertThat(flagged).isEmpty();
        verify(researchChangeRepository, never()).save(any());
    }

    @Test
    void duplicateChangesWithinOneExtractionAreFlaggedOnce() {
        stubMeeting();
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID)).thenReturn(Collections.emptyList());
        when(researchChangeRepository.save(any(ResearchChange.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractedResearchChange first = new ExtractedResearchChange();
        first.setPreviousApproach("PMC-VQA dataset");
        first.setNewApproach("LitMedImage dataset");
        ExtractedResearchChange repeat = new ExtractedResearchChange();
        repeat.setPreviousApproach("PMC-VQA dataset");
        repeat.setNewApproach("LitMedImage dataset");
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setResearchChanges(List.of(first, repeat));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(PROJECT_ID, MEETING_ID, extraction);

        assertThat(flagged).hasSize(1);
        verify(researchChangeRepository, times(1)).save(any(ResearchChange.class));
    }

    @Test
    void longChangeDescriptionsFitTheTitleColumns() {
        stubMeeting();
        when(researchChangeRepository.findByProjectIdOrderByDetectedAtDesc(PROJECT_ID)).thenReturn(Collections.emptyList());
        when(researchChangeRepository.save(any(ResearchChange.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractedResearchChange change = new ExtractedResearchChange();
        change.setDescription("Switch " + "z".repeat(300));
        change.setPreviousApproach("PMC-VQA dataset");
        change.setNewApproach("LitMedImage dataset");
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setResearchChanges(List.of(change));

        List<ResearchChange> flagged = changeDetectionService.detectAndFlagChanges(PROJECT_ID, MEETING_ID, extraction);

        assertThat(flagged.get(0).getTitle()).hasSize(255);
        ArgumentCaptor<ResearchEvent> eventCaptor = ArgumentCaptor.forClass(ResearchEvent.class);
        verify(researchEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTitle()).hasSize(255).startsWith("Flagged Change: Switch zzz");
    }

    @Test
    void updatesChangeStatusSuccessfully() {
        ResearchChange change = new ResearchChange();
        change.setId(42L);
        change.setProjectId(PROJECT_ID);
        change.setStatus(ResearchChangeStatus.FLAGGED);

        when(researchChangeRepository.findById(42L)).thenReturn(Optional.of(change));
        when(researchChangeRepository.save(any(ResearchChange.class))).thenAnswer(inv -> inv.getArgument(0));

        ResearchChange updated = changeDetectionService.updateChangeStatus(PROJECT_ID, 42L, ResearchChangeStatus.REVIEWED);
        assertThat(updated.getStatus()).isEqualTo(ResearchChangeStatus.REVIEWED);
    }

    @Test
    void rejectsUpdateForNonexistentProjectOrChange() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> changeDetectionService.updateChangeStatus(999L, 42L, ResearchChangeStatus.REVIEWED))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Project not found");
    }

    private void stubMeeting() {
        Meeting meeting = new Meeting();
        meeting.setId(MEETING_ID);
        meeting.setProjectId(PROJECT_ID);
        meeting.setTitle("Evaluation review");
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
    }

    private static ResearchDecision activeDecision(Long id, String title, Long meetingId) {
        ResearchDecision decision = new ResearchDecision();
        decision.setId(id);
        decision.setProjectId(PROJECT_ID);
        decision.setMeetingId(meetingId);
        decision.setTitle(title);
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        return decision;
    }

    private static MeetingExtractionResponse extractionWithDecision(String title) {
        ExtractedDecision decision = new ExtractedDecision();
        decision.setTitle(title);
        MeetingExtractionResponse extraction = new MeetingExtractionResponse();
        extraction.setDecisions(List.of(decision));
        return extraction;
    }
}
