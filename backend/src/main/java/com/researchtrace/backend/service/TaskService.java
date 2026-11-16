package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.MilestoneStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.model.TextLimits;
import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ProjectMemberRole;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.MeetingRepository;
import com.researchtrace.backend.repository.ProjectMemberRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.TaskHistoryRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final MilestoneService milestoneService;
    private final TaskSuggestionRepository taskSuggestionRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ResearchDecisionRepository researchDecisionRepository;
    private final ExperimentRepository experimentRepository;
    private final ResearchEventRepository researchEventRepository;
    private final ProgressService progressService;
    private final MeetingRepository meetingRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Autowired
    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResearchEventRepository researchEventRepository,
            @Nullable ProgressService progressService,
            @Nullable MeetingRepository meetingRepository,
            @Nullable ProjectMemberRepository projectMemberRepository) {
        this.taskRepository = taskRepository;
        this.milestoneService = milestoneService;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.researchEventRepository = researchEventRepository;
        this.progressService = progressService;
        this.meetingRepository = meetingRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResearchEventRepository researchEventRepository,
            @Nullable ProgressService progressService) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository,
             researchDecisionRepository, experimentRepository, researchEventRepository,
             progressService, null, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResearchEventRepository researchEventRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository,
             researchDecisionRepository, experimentRepository, researchEventRepository, null, null, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository,
             researchDecisionRepository, experimentRepository, null, null, null, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository,
             null, null, null, null, null, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, null, null, null, null, null, null, null);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task getTaskForProject(Long projectId, Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (projectId == null || !projectId.equals(task.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return task;
    }

    public List<TaskHistory> getTaskHistoryForProject(Long projectId, Long taskId) {
        getTaskForProject(projectId, taskId);
        return getTaskHistory(taskId);
    }

    public List<Task> getTasksByMilestoneForProject(Long projectId, Long milestoneId) {
        Milestone milestone = milestoneService.getMilestoneById(milestoneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        if (projectId == null || !projectId.equals(milestone.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found");
        }
        return taskRepository.findByMilestoneIdAndProjectId(milestoneId, projectId);
    }

    public List<Task> getTasksByAssigneeForProject(Long projectId, Long assigneeId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return taskRepository.findByAssigneeIdAndProjectId(assigneeId, projectId);
    }

    public List<Task> getTasksByResearchDecisionForProject(Long projectId, Long decisionId) {
        ResearchDecision decision = researchDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Research decision not found"));
        if (projectId == null || !projectId.equals(decision.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research decision not found");
        }
        return taskRepository.findByProjectIdAndResearchDecisionId(projectId, decisionId);
    }

    public List<Task> getTasksByExperimentForProject(Long projectId, Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
        if (projectId == null || !projectId.equals(experiment.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found");
        }
        return taskRepository.findByProjectIdAndExperimentId(projectId, experimentId);
    }

    @Transactional
    public Task updateTaskForProject(Long projectId, Long taskId, Task update, Long userId) {
        getTaskForProject(projectId, taskId);
        if (update == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task payload is required");
        }
        if (update.getProjectId() != null && !projectId.equals(update.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        if (update.getTitle() != null) {
            String title = update.getTitle().trim();
            if (title.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title must not be blank");
            }
            update.setTitle(title);
        }
        update.setId(taskId);
        update.setProjectId(projectId);
        return saveTask(update, userId);
    }

    @Transactional
    public void deleteTaskForProject(Long projectId, Long taskId) {
        getTaskForProject(projectId, taskId);
        deleteTask(taskId);
    }

    public List<Task> getTasksByMilestone(Long milestoneId) {
        return taskRepository.findByMilestoneIdOrderByIdAsc(milestoneId);
    }

    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeIdOrderByIdAsc(assigneeId);
    }

    public List<Task> getTasksByResearchDecision(Long researchDecisionId) {
        return taskRepository.findByResearchDecisionId(researchDecisionId);
    }

    public List<Task> getTasksByExperiment(Long experimentId) {
        return taskRepository.findByExperimentId(experimentId);
    }

    public List<Task> getTasksByProject(Long projectId) {
        Map<Long, Task> tasks = new LinkedHashMap<>();

        List<Task> directTasks = taskRepository.findByProjectIdOrderByIdAsc(projectId);
        if (directTasks == null || directTasks.isEmpty()) {
            directTasks = taskRepository.findByProjectId(projectId);
        }
        if (directTasks != null) {
            for (Task task : directTasks) {
                if (belongsToProject(task, projectId)) {
                    tasks.put(task.getId(), task);
                }
            }
        }

        if (milestoneService != null) {
            List<Long> milestoneIds = milestoneService.getMilestonesByProject(projectId).stream()
                .map(Milestone::getId)
                .filter(Objects::nonNull)
                .toList();
            if (!milestoneIds.isEmpty()) {
                for (Task task : taskRepository.findByMilestoneIdIn(milestoneIds)) {
                    if (belongsToProject(task, projectId) && !tasks.containsKey(task.getId())) {
                        tasks.put(task.getId(), task);
                    }
                }
            }
        }

        if (taskSuggestionRepository != null) {
            List<Long> acceptedTaskIds = taskSuggestionRepository
                .findByProjectIdAndStatus(projectId, TaskSuggestionStatus.ACCEPTED).stream()
                .map(TaskSuggestion::getAcceptedTaskId)
                .filter(Objects::nonNull)
                .filter(id -> !tasks.containsKey(id))
                .distinct()
                .toList();
            if (!acceptedTaskIds.isEmpty()) {
                taskRepository.findAllById(acceptedTaskIds)
                    .forEach(task -> {
                        if (belongsToProject(task, projectId) && !tasks.containsKey(task.getId())) {
                            tasks.put(task.getId(), task);
                        }
                    });
            }
        }

        // Return deterministically sorted list by ID ASC so UI ordering is permanent and stable
        return tasks.values().stream()
            .sorted(Comparator.comparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    @Transactional
    public Task createForProject(Long projectId, Task task) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ID is required");
        }
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task payload is required");
        }
        String title = task.getTitle() == null ? "" : task.getTitle().trim();
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title must not be blank");
        }
        task.setTitle(title);
        task.setId(null);
        task.setProjectId(projectId);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        return saveTask(task);
    }

    @Transactional
    public Task saveTask(Task task) {
        return saveTask(task, null);
    }

    @Transactional
    public Task saveTask(Task task, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        TaskStatus previousStatus = null;
        Long previousMilestoneId = null;
        boolean isStatusChanged = false;

        if (task.getId() == null) {
            if (task.getCreatedAt() == null) {
                task.setCreatedAt(now);
            }
            if (task.getUpdatedAt() == null) {
                task.setUpdatedAt(task.getCreatedAt());
            }
        } else {
            Optional<Task> existingOpt = taskRepository.findById(task.getId());
            if (existingOpt.isPresent()) {
                Task existing = existingOpt.get();
                previousStatus = existing.getStatus();
                previousMilestoneId = existing.getMilestoneId();

                if (task.getStatus() != null && task.getStatus() != previousStatus) {
                    isStatusChanged = true;
                }
                if (task.getTitle() == null) {
                    task.setTitle(existing.getTitle());
                }
                if (task.getDescription() == null) {
                    task.setDescription(existing.getDescription());
                }
                if (task.getAssigneeId() == null) {
                    task.setAssigneeId(existing.getAssigneeId());
                }
                if (task.getMilestoneId() == null) {
                    task.setMilestoneId(existing.getMilestoneId());
                }
                if (task.getDueDate() == null) {
                    task.setDueDate(existing.getDueDate());
                }
                if (task.getProjectId() == null) {
                    task.setProjectId(existing.getProjectId());
                }
                if (task.getMeetingId() == null) {
                    task.setMeetingId(existing.getMeetingId());
                }
                if (task.getTaskSuggestionId() == null) {
                    task.setTaskSuggestionId(existing.getTaskSuggestionId());
                }
                if (task.getResearchDecisionId() == null) {
                    task.setResearchDecisionId(existing.getResearchDecisionId());
                }
                if (task.getExperimentId() == null) {
                    task.setExperimentId(existing.getExperimentId());
                }
                if (task.getCreatedAt() == null) {
                    task.setCreatedAt(existing.getCreatedAt());
                }
            }
            task.setUpdatedAt(now);
        }

        validateMilestone(task);
        validateResearchLinks(task);
        validateMeeting(task);
        validateAssignee(task);

        Task saved = taskRepository.save(task);

        if (isStatusChanged && taskHistoryRepository != null) {
            TaskHistory history = new TaskHistory(
                saved.getId(),
                previousStatus,
                saved.getStatus(),
                userId,
                now
            );
            taskHistoryRepository.save(history);
        }

        boolean isCompletedTransition = (saved.getStatus() == TaskStatus.COMPLETED && previousStatus != TaskStatus.COMPLETED);
        if (isCompletedTransition
                && (saved.getResearchDecisionId() != null || saved.getExperimentId() != null)
                && researchEventRepository != null) {
            if (!researchEventRepository.existsByTaskIdAndEventType(saved.getId(), ResearchEventType.TASK_COMPLETED)) {
                ResearchEvent event = new ResearchEvent();
                event.setProjectId(saved.getProjectId());
                event.setTaskId(saved.getId());
                event.setEventType(ResearchEventType.TASK_COMPLETED);
                event.setTitle(TextLimits.title("Task Completed: " + saved.getTitle()));
                String desc = (saved.getDescription() != null && !saved.getDescription().isBlank())
                    ? saved.getDescription()
                    : "Task '" + saved.getTitle() + "' marked as completed.";
                event.setDescription(desc);
                event.setOccurredAt(now);
                event.setCreatedAt(now);
                event.setMeetingId(saved.getMeetingId());
                event.setResearchDecisionId(saved.getResearchDecisionId());
                event.setExperimentId(saved.getExperimentId());
                event.setCreatedByUserId(userId);
                researchEventRepository.save(event);
            }
        }

        // Synchronize affected milestone statuses
        syncMilestoneStatus(saved.getMilestoneId());
        if (previousMilestoneId != null && !previousMilestoneId.equals(saved.getMilestoneId())) {
            syncMilestoneStatus(previousMilestoneId);
        }

        return saved;
    }

    public void syncMilestoneStatus(Long milestoneId) {
        if (milestoneId == null || milestoneService == null) {
            return;
        }
        Optional<Milestone> opt = milestoneService.getMilestoneById(milestoneId);
        if (opt.isEmpty()) {
            return;
        }
        Milestone milestone = opt.get();
        if (milestone.getStatus() == MilestoneStatus.BLOCKED
                || milestone.getStatus() == MilestoneStatus.DEFERRED) {
            return;
        }
        List<Task> tasks = taskRepository.findByMilestoneId(milestoneId);
        if (tasks.isEmpty()) {
            // No associated tasks: preserve existing manual status
            return;
        }

        BigDecimal progress = progressService != null
            ? progressService.calculateTaskProgress(tasks)
            : calculateTaskProgressFallback(tasks);

        if (progress.compareTo(BigDecimal.ZERO) == 0) {
            milestone.setStatus(MilestoneStatus.NOT_STARTED);
        } else if (progress.compareTo(new BigDecimal("100")) >= 0) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
        } else {
            milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        }
        milestoneService.saveMilestone(milestone);
    }

    private BigDecimal calculateTaskProgressFallback(Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Map<Long, Task> unique = new LinkedHashMap<>();
        for (Task t : tasks) {
            if (t != null && t.getId() != null) {
                unique.put(t.getId(), t);
            }
        }
        if (unique.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal pts = BigDecimal.ZERO;
        for (Task t : unique.values()) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                pts = pts.add(new BigDecimal("100"));
            } else if (t.getStatus() == TaskStatus.IN_PROGRESS) {
                pts = pts.add(new BigDecimal("50"));
            }
        }
        return pts.divide(BigDecimal.valueOf(unique.size()), 2, RoundingMode.HALF_UP);
    }

    private static boolean belongsToProject(Task task, Long projectId) {
        return task != null && task.getId() != null && projectId != null && projectId.equals(task.getProjectId());
    }

    private void validateMilestone(Task task) {
        if (task.getMilestoneId() != null && milestoneService != null) {
            Optional<Milestone> milestoneOpt = milestoneService.getMilestoneById(task.getMilestoneId());
            if (milestoneOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone not found: " + task.getMilestoneId());
            }
            Milestone milestone = milestoneOpt.get();
            if (task.getProjectId() == null) {
                task.setProjectId(milestone.getProjectId());
            } else if (!Objects.equals(task.getProjectId(), milestone.getProjectId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone must belong to the task's project");
            }
        }
    }

    private void validateResearchLinks(Task task) {
        if (task.getProjectId() != null) {
            if (task.getResearchDecisionId() != null && researchDecisionRepository != null) {
                boolean valid = researchDecisionRepository.findById(task.getResearchDecisionId())
                    .filter(decision -> Objects.equals(task.getProjectId(), decision.getProjectId()))
                    .isPresent();
                if (!valid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Research decision must belong to the task's project");
                }
            }
            if (task.getExperimentId() != null && experimentRepository != null) {
                boolean valid = experimentRepository.findById(task.getExperimentId())
                    .filter(exp -> Objects.equals(task.getProjectId(), exp.getProjectId()))
                    .isPresent();
                if (!valid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experiment must belong to the task's project");
                }
            }
        }
    }

    private void validateMeeting(Task task) {
        if (task.getMeetingId() == null || meetingRepository == null) {
            return;
        }
        Meeting meeting = meetingRepository.findById(task.getMeetingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting not found: " + task.getMeetingId()));
        if (task.getProjectId() != null && !task.getProjectId().equals(meeting.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting must belong to the task's project");
        }
    }

    private void validateAssignee(Task task) {
        if (task.getAssigneeId() == null || projectMemberRepository == null || task.getProjectId() == null) {
            return;
        }
        boolean student = projectMemberRepository.findByProjectId(task.getProjectId()).stream()
            .anyMatch(membership -> task.getAssigneeId().equals(membership.getUserId())
                && membership.getRole() == ProjectMemberRole.STUDENT);
        if (!student) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must be a student member of the task's project");
        }
    }

    public List<TaskHistory> getTaskHistory(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        if (taskHistoryRepository == null) {
            return List.of();
        }
        return taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(taskId);
    }

    @Transactional
    public void deleteTask(Long id) {
        if (id == null) {
            return;
        }
        Optional<Task> opt = taskRepository.findById(id);
        if (opt.isPresent()) {
            Long milestoneId = opt.get().getMilestoneId();
            taskRepository.deleteById(id);
            syncMilestoneStatus(milestoneId);
        }
    }
}
