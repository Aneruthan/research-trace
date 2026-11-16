package com.researchtrace.backend.service;

import com.researchtrace.backend.ai.ExtractedTask;
import com.researchtrace.backend.ai.TaskExtractionException;
import com.researchtrace.backend.ai.TaskExtractionInput;
import com.researchtrace.backend.ai.TaskExtractionInput.MilestoneCandidate;
import com.researchtrace.backend.ai.TaskExtractionInput.StudentCandidate;
import com.researchtrace.backend.ai.TaskExtractor;
import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest.ReviewedField;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskSuggestionService {

    static final int MAX_TEXT_LENGTH = 255;

    private static final Logger log = LoggerFactory.getLogger(TaskSuggestionService.class);

    private static final int CONFIDENCE_SCALE = 2;

    private final MeetingRepository meetingRepository;
    private final TaskSuggestionRepository taskSuggestionRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final TaskExtractor taskExtractor;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final TaskService taskService;

    @Autowired
    public TaskSuggestionService(
            MeetingRepository meetingRepository,
            TaskSuggestionRepository taskSuggestionRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            MilestoneRepository milestoneRepository,
            TaskRepository taskRepository,
            TaskExtractor taskExtractor,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            TaskService taskService) {

        this.meetingRepository = meetingRepository;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.taskExtractor = taskExtractor;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.taskService = taskService;
    }

    public TaskSuggestionService(
            MeetingRepository meetingRepository,
            TaskSuggestionRepository taskSuggestionRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            MilestoneRepository milestoneRepository,
            TaskRepository taskRepository,
            TaskExtractor taskExtractor) {

        this(meetingRepository, taskSuggestionRepository, projectMemberRepository, userRepository,
             milestoneRepository, taskRepository, taskExtractor, null, null, null);
    }

    @Transactional
    public List<TaskSuggestion> extract(Long projectId, Long meetingId) {
        requireMeetingInProject(projectId, meetingId);
        return extract(meetingId);
    }

    @Transactional
    public List<TaskSuggestion> extract(Long meetingId) {
        Meeting meeting = findMeeting(meetingId);
        Long projectId = meeting.getProjectId();
        if (projectId == null) {
            throw badRequest("Meeting is not linked to a project");
        }

        if (meeting.getNotes() == null || meeting.getNotes().isBlank()) {
            throw badRequest("Meeting notes are required before task extraction");
        }

        List<StudentCandidate> students = loadStudents(projectId);
        List<MilestoneCandidate> milestones = loadMilestones(projectId);

        List<ExtractedTask> extracted = taskExtractor.extract(new TaskExtractionInput(
            meeting.getTitle(),
            meeting.getMeetingDate(),
            meeting.getNotes(),
            students,
            milestones));

        Set<Long> studentIds = students.stream()
            .map(StudentCandidate::id)
            .collect(Collectors.toSet());
        Set<Long> milestoneIds = milestones.stream()
            .map(MilestoneCandidate::id)
            .collect(Collectors.toSet());

        if (extracted == null) {
            throw new TaskExtractionException("Task extraction failed. The model response could not be used.");
        }

        List<TaskSuggestion> suggestions = new ArrayList<>();
        for (ExtractedTask task : extracted) {
            TaskSuggestion suggestion = toPendingSuggestion(
                task, meetingId, projectId, studentIds, milestoneIds);
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        List<TaskSuggestion> unique = withoutReviewedDuplicates(meetingId, suggestions);
        log.info("Task extraction for meeting {} produced {} suggestion(s)", meetingId, unique.size());
        taskSuggestionRepository.deleteByMeetingIdAndStatus(meetingId, TaskSuggestionStatus.PENDING);
        return taskSuggestionRepository.saveAll(unique);
    }

    private List<TaskSuggestion> withoutReviewedDuplicates(Long meetingId, List<TaskSuggestion> suggestions) {
        Set<String> blockedTitles = new HashSet<>();
        List<TaskSuggestion> existingSuggestions = taskSuggestionRepository.findByMeetingId(meetingId);
        if (existingSuggestions != null) {
            for (TaskSuggestion existing : existingSuggestions) {
                if (existing != null && existing.getStatus() == TaskSuggestionStatus.ACCEPTED) {
                    addTitle(blockedTitles, existing.getTitle());
                }
            }
        }
        List<Task> existingTasks = taskRepository.findByMeetingId(meetingId);
        if (existingTasks != null) {
            for (Task task : existingTasks) {
                if (task != null) {
                    addTitle(blockedTitles, task.getTitle());
                }
            }
        }

        List<TaskSuggestion> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TaskSuggestion suggestion : suggestions) {
            String key = normalizedTitle(suggestion.getTitle());
            if (key == null || blockedTitles.contains(key) || !seen.add(key)) {
                continue;
            }
            unique.add(suggestion);
        }
        return unique;
    }

    private void addTitle(Set<String> titles, String title) {
        String key = normalizedTitle(title);
        if (key != null) {
            titles.add(key);
        }
    }

    private String normalizedTitle(String title) {
        String trimmed = trimToNull(title);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<TaskSuggestion> listByMeeting(Long projectId, Long meetingId) {
        requireMeetingInProject(projectId, meetingId);
        return listByMeeting(meetingId);
    }

    @Transactional(readOnly = true)
    public List<TaskSuggestion> listByMeeting(Long meetingId) {
        findMeeting(meetingId);
        return taskSuggestionRepository.findByMeetingId(meetingId).stream()
            .sorted(Comparator.comparing(
                TaskSuggestion::getId,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    @Transactional
    public TaskSuggestion update(Long projectId, Long meetingId, Long suggestionId, AcceptTaskSuggestionRequest request) {
        requireSuggestionInProject(projectId, meetingId, suggestionId);
        return update(suggestionId, request);
    }

    @Transactional
    public TaskSuggestion update(Long suggestionId, AcceptTaskSuggestionRequest request) {
        TaskSuggestion suggestion = findPendingSuggestion(suggestionId);
        applyReviewedValues(suggestion, request);
        return taskSuggestionRepository.save(suggestion);
    }

    @Transactional
    public Task accept(Long projectId, Long meetingId, Long suggestionId, AcceptTaskSuggestionRequest request) {
        requireSuggestionInProject(projectId, meetingId, suggestionId);
        return accept(suggestionId, request);
    }

    @Transactional
    public Task accept(Long suggestionId, AcceptTaskSuggestionRequest request) {
        TaskSuggestion suggestion = findPendingSuggestion(suggestionId);
        applyReviewedValues(suggestion, request);

        Task task = new Task();
        task.setTitle(suggestion.getTitle());
        task.setDescription(suggestion.getDescription());
        task.setAssigneeId(suggestion.getAssigneeId());
        task.setMilestoneId(suggestion.getMilestoneId());
        task.setDueDate(suggestion.getDueDate());
        task.setProjectId(suggestion.getProjectId());
        task.setMeetingId(suggestion.getMeetingId());
        task.setTaskSuggestionId(suggestion.getId());
        task.setResearchDecisionId(suggestion.getResearchDecisionId());
        task.setExperimentId(suggestion.getExperimentId());
        task.setStatus(TaskStatus.TODO);
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        // Guard against duplicate task creation if a task for this suggestion already exists
        List<Task> existingForSuggestion = taskRepository.findByTaskSuggestionId(suggestionId);
        if (existingForSuggestion != null && !existingForSuggestion.isEmpty()) {
            Task existingTask = existingForSuggestion.get(0);
            suggestion.setStatus(TaskSuggestionStatus.ACCEPTED);
            suggestion.setAcceptedTaskId(existingTask.getId());
            taskSuggestionRepository.save(suggestion);
            syncAcceptedMilestone(existingTask.getMilestoneId());
            return existingTask;
        }

        Task savedTask = taskRepository.save(task);

        suggestion.setStatus(TaskSuggestionStatus.ACCEPTED);
        suggestion.setAcceptedTaskId(savedTask.getId());
        taskSuggestionRepository.save(suggestion);
        syncAcceptedMilestone(savedTask.getMilestoneId());

        return savedTask;
    }

    @Transactional
    public TaskSuggestion reject(Long projectId, Long meetingId, Long suggestionId) {
        requireSuggestionInProject(projectId, meetingId, suggestionId);
        return reject(suggestionId);
    }

    @Transactional
    public TaskSuggestion reject(Long suggestionId) {
        TaskSuggestion suggestion = findPendingSuggestion(suggestionId);
        suggestion.setStatus(TaskSuggestionStatus.REJECTED);
        return taskSuggestionRepository.save(suggestion);
    }

    private TaskSuggestion toPendingSuggestion(
            ExtractedTask task,
            Long meetingId,
            Long projectId,
            Set<Long> studentIds,
            Set<Long> milestoneIds) {

        if (task == null) {
            return null;
        }

        String title = truncate(trimToNull(task.title()));
        if (title == null) {
            return null;
        }

        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setMeetingId(meetingId);
        suggestion.setProjectId(projectId);
        suggestion.setTitle(title);
        suggestion.setDescription(truncate(trimToNull(task.description())));
        suggestion.setAssigneeName(truncate(trimToNull(task.assigneeName())));
        suggestion.setAssigneeId(studentIds.contains(task.assigneeId()) ? task.assigneeId() : null);
        suggestion.setMilestoneId(milestoneIds.contains(task.milestoneId()) ? task.milestoneId() : null);
        suggestion.setDueDate(task.dueDate());
        suggestion.setSourceExcerpt(trimToNull(task.sourceExcerpt()));
        suggestion.setConfidence(isValidConfidence(task.confidence())
            ? scaleConfidence(task.confidence())
            : null);
        suggestion.setStatus(TaskSuggestionStatus.PENDING);
        return suggestion;
    }

    private void applyReviewedValues(TaskSuggestion suggestion, AcceptTaskSuggestionRequest request) {
        String title = suggestion.getTitle();
        String description = suggestion.getDescription();
        Long assigneeId = suggestion.getAssigneeId();
        Long milestoneId = suggestion.getMilestoneId();
        LocalDate dueDate = suggestion.getDueDate();
        Long researchDecisionId = suggestion.getResearchDecisionId();
        Long experimentId = suggestion.getExperimentId();

        boolean assigneeChanged = false;
        if (request != null) {
            if (request.has(ReviewedField.TITLE)) {
                title = request.title();
            }
            if (request.has(ReviewedField.DESCRIPTION)) {
                description = request.description();
            }
            if (request.has(ReviewedField.ASSIGNEE_ID)) {
                assigneeId = request.assigneeId();
                assigneeChanged = true;
            }
            if (request.has(ReviewedField.MILESTONE_ID)) {
                milestoneId = request.milestoneId();
            }
            if (request.has(ReviewedField.DUE_DATE)) {
                dueDate = request.dueDate();
            }
            if (request.has(ReviewedField.RESEARCH_DECISION_ID)) {
                researchDecisionId = request.researchDecisionId();
            }
            if (request.has(ReviewedField.EXPERIMENT_ID)) {
                experimentId = request.experimentId();
            }
        }

        String validTitle = requireTitle(title);
        String validDescription = optionalText(description, "Description");
        Long validAssigneeId = requireProjectStudent(suggestion.getProjectId(), assigneeId);
        Long validMilestoneId = requireProjectMilestone(suggestion.getProjectId(), milestoneId);
        Long validResearchDecisionId = requireProjectResearchDecision(suggestion.getProjectId(), researchDecisionId);
        Long validExperimentId = requireProjectExperiment(suggestion.getProjectId(), experimentId);
        if (!isValidConfidence(suggestion.getConfidence())) {
            throw badRequest("Confidence must be between 0 and 1");
        }

        suggestion.setTitle(validTitle);
        suggestion.setDescription(validDescription);
        suggestion.setAssigneeId(validAssigneeId);
        if (assigneeChanged) {
            suggestion.setAssigneeName(assigneeName(validAssigneeId));
        }
        suggestion.setMilestoneId(validMilestoneId);
        suggestion.setDueDate(dueDate);
        suggestion.setResearchDecisionId(validResearchDecisionId);
        suggestion.setExperimentId(validExperimentId);
    }

    private String assigneeName(Long assigneeId) {
        if (assigneeId == null || userRepository == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
            .map(user -> user.getName())
            .orElse(null);
    }

    private void syncAcceptedMilestone(Long milestoneId) {
        if (taskService != null) {
            taskService.syncMilestoneStatus(milestoneId);
        }
    }

    private String requireTitle(String value) {
        String title = trimToNull(value);
        if (title == null) {
            throw badRequest("Title must not be blank");
        }
        return requireMaxLength(title, "Title");
    }

    private String optionalText(String value, String fieldName) {
        String text = trimToNull(value);
        return text == null ? null : requireMaxLength(text, fieldName);
    }

    private String requireMaxLength(String value, String fieldName) {
        if (value.length() > MAX_TEXT_LENGTH) {
            throw badRequest(fieldName + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return value;
    }

    private Long requireProjectStudent(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        boolean isProjectStudent = projectId != null && loadStudents(projectId).stream()
            .anyMatch(student -> assigneeId.equals(student.id()));
        if (!isProjectStudent) {
            throw badRequest("Assignee must be a student member of the meeting's project");
        }
        return assigneeId;
    }

    private Long requireProjectMilestone(Long projectId, Long milestoneId) {
        if (milestoneId == null) {
            return null;
        }
        boolean belongsToProject = projectId != null && milestoneRepository.findById(milestoneId)
            .filter(milestone -> Objects.equals(projectId, milestone.getProjectId()))
            .isPresent();
        if (!belongsToProject) {
            throw badRequest("Milestone must belong to the meeting's project");
        }
        return milestoneId;
    }

    private Long requireProjectResearchDecision(Long projectId, Long decisionId) {
        if (decisionId == null) {
            return null;
        }
        if (researchDecisionRepository == null) {
            return decisionId;
        }
        boolean belongsToProject = projectId != null && researchDecisionRepository.findById(decisionId)
            .filter(decision -> Objects.equals(projectId, decision.getProjectId()))
            .isPresent();
        if (!belongsToProject) {
            throw badRequest("Research decision must belong to the meeting's project");
        }
        return decisionId;
    }

    private Long requireProjectExperiment(Long projectId, Long experimentId) {
        if (experimentId == null) {
            return null;
        }
        if (experimentRepository == null) {
            return experimentId;
        }
        boolean belongsToProject = projectId != null && experimentRepository.findById(experimentId)
            .filter(exp -> Objects.equals(projectId, exp.getProjectId()))
            .isPresent();
        if (!belongsToProject) {
            throw badRequest("Experiment must belong to the meeting's project");
        }
        return experimentId;
    }

    private List<StudentCandidate> loadStudents(Long projectId) {
        List<Long> studentIds = projectMemberRepository.findByProjectId(projectId).stream()
            .filter(member -> member.getRole() == ProjectMemberRole.STUDENT)
            .map(ProjectMember::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(studentIds).stream()
            .map(user -> new StudentCandidate(user.getId(), user.getName()))
            .sorted(Comparator.comparing(StudentCandidate::id))
            .toList();
    }

    private List<MilestoneCandidate> loadMilestones(Long projectId) {
        return milestoneRepository.findByProjectId(projectId).stream()
            .map(milestone -> new MilestoneCandidate(milestone.getId(), milestone.getTitle()))
            .sorted(Comparator.comparing(MilestoneCandidate::id))
            .toList();
    }

    private void requireMeetingInProject(Long projectId, Long meetingId) {
        Meeting meeting = findMeeting(meetingId);
        if (projectId == null || !projectId.equals(meeting.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found");
        }
    }

    private void requireSuggestionInProject(Long projectId, Long meetingId, Long suggestionId) {
        requireMeetingInProject(projectId, meetingId);
        TaskSuggestion suggestion = taskSuggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task suggestion not found"));
        if (!meetingId.equals(suggestion.getMeetingId()) || !projectId.equals(suggestion.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task suggestion not found");
        }
    }

    private Meeting findMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    private TaskSuggestion findPendingSuggestion(Long suggestionId) {
        TaskSuggestion suggestion = taskSuggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Task suggestion not found"));
        if (suggestion.getStatus() != TaskSuggestionStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Only PENDING task suggestions can be reviewed");
        }
        return suggestion;
    }

    private boolean isValidConfidence(BigDecimal confidence) {
        return confidence == null
            || (confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0);
    }

    private BigDecimal scaleConfidence(BigDecimal confidence) {
        return confidence == null ? null : confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
