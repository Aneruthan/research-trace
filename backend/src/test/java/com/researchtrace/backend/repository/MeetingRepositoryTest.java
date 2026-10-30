package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.Meeting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsLongNotesWithoutTruncation() {
        String notes = "Aneruthan will run the Qwen baseline by next Friday. ".repeat(400);
        assertThat(notes.length()).isGreaterThan(20_000);

        Meeting meeting = new Meeting();
        meeting.setProjectId(1L);
        meeting.setTitle("Weekly sync");
        meeting.setMeetingDate(LocalDate.of(2026, 10, 2));
        meeting.setNotes(notes);

        Long id = meetingRepository.save(meeting).getId();
        entityManager.flush();
        entityManager.clear();

        Meeting reloaded = meetingRepository.findById(id).orElseThrow();

        assertThat(reloaded.getNotes()).isEqualTo(notes);
        assertThat(reloaded.getNotes()).hasSize(notes.length());
    }

    @Test
    void findsMeetingsOfOneProjectOnly() {
        meetingRepository.save(meeting(1L, "Weekly sync"));
        meetingRepository.save(meeting(1L, "Design review"));
        meetingRepository.save(meeting(2L, "Other project"));
        entityManager.flush();
        entityManager.clear();

        assertThat(meetingRepository.findByProjectId(1L))
            .extracting(Meeting::getTitle)
            .containsExactlyInAnyOrder("Weekly sync", "Design review");
    }

    private Meeting meeting(Long projectId, String title) {
        Meeting meeting = new Meeting();
        meeting.setProjectId(projectId);
        meeting.setTitle(title);
        meeting.setMeetingDate(LocalDate.of(2026, 10, 2));
        meeting.setNotes("Notes");
        return meeting;
    }
}
