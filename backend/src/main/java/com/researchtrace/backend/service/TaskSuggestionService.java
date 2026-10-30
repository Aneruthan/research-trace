package com.researchtrace.backend.service;

import com.researchtrace.backend.ai.ExtractedTask;
import com.researchtrace.backend.ai.TaskExtractionInput;
import com.researchtrace.backend.ai.TaskExtractionInput.MilestoneCandidate;
import com.researchtrace.backend.ai.TaskExtractionInput.StudentCandidate;
import com.researchtrace.backend.ai.TaskExtractor;
import com.researchtrace.backend.dto.AcceptTaskSuggestionRequest;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.MilestoneRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import com.researchtrace.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskSuggestionService {

    static final int MAX_TEXT_LENGTH = 255;

    private static final int CONFIDENCE_SCALE = 2;

    private final MeetingRepository meetingRepository;
    private final TaskSuggestionRepository taskSuggestionRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final TaskExtractor taskExtractor;

    public TaskSuggestionService(
            MeetingRepository meetingRepository,
            TaskSuggestionRepository taskSuggestionRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            MilestoneRepository milestoneRepository,
            TaskRepository taskRepository,
            TaskExtractor taskExtractor) {

        this.meetingRepository = meetingRepository;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.taskExtractor = taskExtractor;
    }

    @Transactional
    public List<TaskSuggestion> extract(Long meetingId) {
        Meeting meeting = findMeeting(meetingId);
        Long projectId = meeting.getProjectId();
        if (projectId == null) {
            throw badRequest("Meeting is not linked to a project");
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

        List<TaskSuggestion> suggestions = new ArrayList<>();
        if (extracted != null) {
            for (ExtractedTask task : extracted) {
                TaskSuggestion suggestion = toPendingSuggestion(
                    task, meetingId, projectId, studentIds, milestoneIds);
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
        }

        taskSuggestionRepository.deleteByMeetingIdAndStatus(meetingId, TaskSuggestionStatus.PENDING);
        return taskSuggestionRepository.saveAll(suggestions);
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
    public TaskSuggestion update(Long suggestionId, AcceptTaskSuggestionRequest request) {
        TaskSuggestion suggestion = findPendingSuggestion(suggestionId);
        applyReviewedValues(suggestion, request);
        return taskSuggestionRepository.save(suggestion);
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
        task.setStatus(TaskStatus.TODO);
        Task savedTask = taskRepository.save(task);

        suggestion.setStatus(TaskSuggestionStatus.ACCEPTED);
        suggestion.setAcceptedTaskId(savedTask.getId());
        taskSuggestionRepository.save(suggestion);

        return savedTask;
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

        if (request != null) {
            if (request.title() != null) {
                title = request.title();
            }
            if (request.description() != null) {
                description = request.description();
            }
            if (request.assigneeId() != null) {
                assigneeId = request.assigneeId();
            }
            if (request.milestoneId() != null) {
                milestoneId = request.milestoneId();
            }
            if (request.dueDate() != null) {
                dueDate = request.dueDate();
            }
        }

        String validTitle = requireTitle(title);
        String validDescription = optionalText(description, "Description");
        Long validAssigneeId = requireProjectStudent(suggestion.getProjectId(), assigneeId);
        Long validMilestoneId = requireProjectMilestone(suggestion.getProjectId(), milestoneId);
        if (!isValidConfidence(suggestion.getConfidence())) {
            throw badRequest("Confidence must be between 0 and 1");
        }

        suggestion.setTitle(validTitle);
        suggestion.setDescription(validDescription);
        suggestion.setAssigneeId(validAssigneeId);
        suggestion.setMilestoneId(validMilestoneId);
        suggestion.setDueDate(dueDate);
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
