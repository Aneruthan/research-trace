package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.TextLimits;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ExtractionReviewRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectRepository;
import com.researchtrace.backend.repository.ResearchChangeRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.ResultRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskSuggestionRepository taskSuggestionRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResultRepository resultRepository;
    private final ExtractionReviewRepository extractionReviewRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ResearchChangeRepository researchChangeRepository;

    @Autowired
    public MeetingService(
            MeetingRepository meetingRepository,
            @Nullable ProjectRepository projectRepository,
            @Nullable TaskRepository taskRepository,
            @Nullable TaskSuggestionRepository taskSuggestionRepository,
            @Nullable ResearchDecisionRepository researchDecisionRepository,
            @Nullable ExperimentRepository experimentRepository,
            @Nullable ResultRepository resultRepository,
            @Nullable ExtractionReviewRepository extractionReviewRepository,
            @Nullable ResearchEventRepository researchEventRepository,
            @Nullable ResearchChangeRepository researchChangeRepository) {
        this.meetingRepository = meetingRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.resultRepository = resultRepository;
        this.extractionReviewRepository = extractionReviewRepository;
        this.researchEventRepository = researchEventRepository;
        this.researchChangeRepository = researchChangeRepository;
    }

    public MeetingService(
            MeetingRepository meetingRepository,
            @Nullable ProjectRepository projectRepository) {
        this(meetingRepository, projectRepository, null, null, null, null, null, null, null, null);
    }

    public MeetingService(MeetingRepository meetingRepository) {
        this(meetingRepository, null, null, null, null, null, null, null, null, null);
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
        rejectLongTitle(meeting.getTitle());
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
            rejectLongTitle(update.getTitle());
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
        deleteMeeting(id, null);
    }

    public void deleteMeeting(Long id, @Nullable Long expectedProjectId) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting ID is required");
        }
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found: " + id));

        if (expectedProjectId != null && !expectedProjectId.equals(meeting.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting does not belong to project: " + expectedProjectId);
        }

        // Safety check: preserve referential integrity and research provenance.
        // Never delete a meeting that has accepted research decisions, experiments, results, tasks, or reviews.
        List<String> dependents = new ArrayList<>();
        if (taskRepository != null && !taskRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("tasks");
        }
        if (taskSuggestionRepository != null && !taskSuggestionRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("task suggestions");
        }
        if (researchDecisionRepository != null && !researchDecisionRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("research decisions");
        }
        if (experimentRepository != null && !experimentRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("experiments");
        }
        if (resultRepository != null && !resultRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("results");
        }
        if (extractionReviewRepository != null && !extractionReviewRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("extraction reviews");
        }
        if (researchEventRepository != null && !researchEventRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("research events");
        }
        if (researchChangeRepository != null && !researchChangeRepository.findByMeetingId(id).isEmpty()) {
            dependents.add("research changes");
        }

        if (!dependents.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot delete meeting " + id + " ('" + meeting.getTitle() + "') because it has linked research records (" +
                String.join(", ", dependents) + "). Deletion rejected to protect research provenance."
            );
        }

        meetingRepository.delete(meeting);
    }

    private static void rejectLongTitle(String title) {
        if (title != null && title.length() > TextLimits.TITLE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 255 characters");
        }
    }
}
