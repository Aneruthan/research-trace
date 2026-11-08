package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public MeetingService(
            MeetingRepository meetingRepository,
            @Nullable ProjectRepository projectRepository) {
        this.meetingRepository = meetingRepository;
        this.projectRepository = projectRepository;
    }

    public MeetingService(MeetingRepository meetingRepository) {
        this(meetingRepository, null);
    }

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    public List<Meeting> getMeetingsByProject(Long projectId) {
        return meetingRepository.findByProjectId(projectId);
    }

    public Optional<Meeting> getMeetingById(Long id) {
        return meetingRepository.findById(id);
    }

    public Meeting createMeeting(Long projectId, Meeting meeting) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ID is required");
        }
        if (projectRepository != null && !projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        if (meeting == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting payload is required");
        }
        if (meeting.getTitle() == null || meeting.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting title must not be blank");
        }
        if (meeting.getMeetingDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting date must not be null");
        }
        meeting.setId(null);
        meeting.setProjectId(projectId);
        return meetingRepository.save(meeting);
    }

    public Meeting updateMeeting(Long id, Meeting update) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting ID is required");
        }
        Meeting existing = meetingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + id));

        if (update == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Update payload is required");
        }

        // Project isolation: reject cross-project reassignment
        if (update.getProjectId() != null && !update.getProjectId().equals(existing.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change meeting project assignment");
        }

        if (update.getTitle() != null) {
            if (update.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting title must not be blank");
            }
            existing.setTitle(update.getTitle());
        }

        if (update.getMeetingDate() != null) {
            existing.setMeetingDate(update.getMeetingDate());
        }

        if (update.getNotes() != null) {
            existing.setNotes(update.getNotes());
        }

        return meetingRepository.save(existing);
    }

    public Meeting saveMeeting(Meeting meeting) {
        if (meeting == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting must not be null");
        }
        if (meeting.getId() != null) {
            return updateMeeting(meeting.getId(), meeting);
        }
        return createMeeting(meeting.getProjectId(), meeting);
    }

    public void deleteMeeting(Long id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + id);
        }
        meetingRepository.deleteById(id);
    }
}
