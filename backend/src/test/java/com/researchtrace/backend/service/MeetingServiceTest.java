package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        meetingService = new MeetingService(
                meetingRepository,
                projectRepository,
                taskRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void createMeeting_succeeds_whenValid() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> {
            Meeting m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        Meeting input = new Meeting();
        input.setTitle("Weekly Sync");
        input.setMeetingDate(LocalDate.of(2026, 11, 8));
        input.setNotes("Discussed ViT architecture.");

        Meeting saved = meetingService.createMeeting(1L, input);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getProjectId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("Weekly Sync");
        assertThat(saved.getMeetingDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(saved.getNotes()).isEqualTo("Discussed ViT architecture.");
    }

    @Test
    void createMeeting_throwsNotFound_whenProjectDoesNotExist() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        Meeting input = new Meeting();
        input.setTitle("Weekly Sync");
        input.setMeetingDate(LocalDate.now());

        assertThatThrownBy(() -> meetingService.createMeeting(999L, input))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createMeeting_throwsBadRequest_whenTitleIsBlankOrNull() {
        when(projectRepository.existsById(1L)).thenReturn(true);

        Meeting input = new Meeting();
        input.setTitle("   ");
        input.setMeetingDate(LocalDate.now());

        assertThatThrownBy(() -> meetingService.createMeeting(1L, input))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createMeeting_throwsBadRequest_whenDateIsNull() {
        when(projectRepository.existsById(1L)).thenReturn(true);

        Meeting input = new Meeting();
        input.setTitle("Weekly Sync");
        input.setMeetingDate(null);

        assertThatThrownBy(() -> meetingService.createMeeting(1L, input))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getMeetingsByProjectDoesNotReadEveryMeeting() {
        Meeting owned = new Meeting();
        owned.setId(1L);
        owned.setProjectId(1L);
        owned.setTitle("Project A");
        when(meetingRepository.findByProjectId(1L)).thenReturn(List.of(owned));

        assertThat(meetingService.getMeetingsByProject(1L)).containsExactly(owned);
        verify(meetingRepository, never()).findAll();
    }

    @Test
    void updateMeetingForProjectRejectsAnotherProjectsMeeting() {
        Meeting existing = new Meeting();
        existing.setId(5L);
        existing.setProjectId(2L);
        when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> meetingService.updateMeeting(1L, 5L, new Meeting()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(meetingRepository, never()).save(any());
    }

    @Test
    void updateMeeting_mergesFieldsAndPreservesProjectId() {
        Meeting existing = new Meeting();
        existing.setId(5L);
        existing.setProjectId(1L);
        existing.setTitle("Old Title");
        existing.setMeetingDate(LocalDate.of(2026, 10, 1));
        existing.setNotes("Old notes");

        when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

        Meeting update = new Meeting();
        update.setNotes("Updated and expanded supervision notes.");

        Meeting result = meetingService.updateMeeting(5L, update);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getProjectId()).isEqualTo(1L); // preserved
        assertThat(result.getTitle()).isEqualTo("Old Title"); // unchanged
        assertThat(result.getMeetingDate()).isEqualTo(LocalDate.of(2026, 10, 1)); // unchanged
        assertThat(result.getNotes()).isEqualTo("Updated and expanded supervision notes.");
    }

    @Test
    void updateMeeting_rejectsCrossProjectReassignment() {
        Meeting existing = new Meeting();
        existing.setId(5L);
        existing.setProjectId(1L);
        existing.setTitle("Old Title");
        existing.setMeetingDate(LocalDate.now());

        when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

        Meeting update = new Meeting();
        update.setProjectId(2L); // Different project!

        assertThatThrownBy(() -> meetingService.updateMeeting(5L, update))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateMeeting_throwsNotFound_whenMeetingDoesNotExist() {
        when(meetingRepository.findById(404L)).thenReturn(Optional.empty());

        Meeting update = new Meeting();
        update.setTitle("New title");

        assertThatThrownBy(() -> meetingService.updateMeeting(404L, update))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteMeeting_succeeds_whenNoDependentRecords() {
        Meeting existing = new Meeting();
        existing.setId(10L);
        existing.setProjectId(1L);
        existing.setTitle("Standalone Meeting");

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(taskRepository.findByMeetingId(10L)).thenReturn(List.of());

        meetingService.deleteMeeting(10L, 1L);

        verify(meetingRepository).delete(existing);
    }

    @Test
    void deleteMeeting_throwsNotFound_whenMeetingDoesNotExist() {
        when(meetingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.deleteMeeting(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteMeeting_rejectsDeletion_whenMeetingHasDependentRecords() {
        Meeting existing = new Meeting();
        existing.setId(7L);
        existing.setProjectId(1L);
        existing.setTitle("ViT-B16 Supervision Meeting");

        when(meetingRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(taskRepository.findByMeetingId(7L)).thenReturn(List.of(new Task()));

        assertThatThrownBy(() -> meetingService.deleteMeeting(7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).contains("tasks");
                    assertThat(ex.getMessage()).contains("Deletion rejected to protect research provenance");
                });

        verify(meetingRepository, never()).delete(any());
    }

    @Test
    void deleteMeeting_rejectsCrossProjectDeletion() {
        Meeting existing = new Meeting();
        existing.setId(10L);
        existing.setProjectId(1L);

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> meetingService.deleteMeeting(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(meetingRepository, never()).delete(any());
    }

    @Test
    void createMeeting_rejectsATitleLongerThan255BeforeSaving() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        Meeting input = new Meeting();
        input.setTitle("m".repeat(256));
        input.setMeetingDate(LocalDate.of(2026, 11, 8));

        assertThatThrownBy(() -> meetingService.createMeeting(1L, input))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(ex.getMessage()).contains("at most 255 characters");
            });
        verify(meetingRepository, never()).save(any());
    }

    @Test
    void updateMeeting_rejectsATitleLongerThan255BeforeSaving() {
        Meeting existing = new Meeting();
        existing.setId(5L);
        existing.setProjectId(1L);
        existing.setTitle("Weekly Sync");
        when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
        Meeting update = new Meeting();
        update.setTitle("m".repeat(256));

        assertThatThrownBy(() -> meetingService.updateMeeting(5L, update))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("at most 255 characters");
        verify(meetingRepository, never()).save(any());
    }

    @Test
    void getMeetingForProjectReturnsTheMeetingWhenItBelongsToTheProject() {
        Meeting meeting = new Meeting();
        meeting.setId(7L);
        meeting.setProjectId(1L);
        meeting.setTitle("ViT review");
        when(meetingRepository.findById(7L)).thenReturn(Optional.of(meeting));

        Meeting loaded = meetingService.getMeetingForProject(1L, 7L);

        assertThat(loaded.getTitle()).isEqualTo("ViT review");
    }

    @Test
    void getMeetingForProjectRejectsAMeetingFromAnotherProject() {
        Meeting meeting = new Meeting();
        meeting.setId(7L);
        meeting.setProjectId(2L);
        when(meetingRepository.findById(7L)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.getMeetingForProject(1L, 7L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getMeetingForProjectRejectsAMissingMeeting() {
        when(meetingRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeetingForProject(1L, 7L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
