package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.ResearchEventType;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.ExperimentRepository;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.TaskHistoryRepository;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    @Autowired
    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository,
            ResearchEventRepository researchEventRepository) {
        this.taskRepository = taskRepository;
        this.milestoneService = milestoneService;
        this.taskSuggestionRepository = taskSuggestionRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.researchDecisionRepository = researchDecisionRepository;
        this.experimentRepository = experimentRepository;
        this.researchEventRepository = researchEventRepository;
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository,
            ResearchDecisionRepository researchDecisionRepository,
            ExperimentRepository experimentRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository, researchDecisionRepository, experimentRepository, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository,
            TaskHistoryRepository taskHistoryRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, taskHistoryRepository, null, null, null);
    }

    public TaskService(
            TaskRepository taskRepository,
            MilestoneService milestoneService,
            TaskSuggestionRepository taskSuggestionRepository) {
        this(taskRepository, milestoneService, taskSuggestionRepository, null, null, null, null);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByMilestone(Long milestoneId) {
        return taskRepository.findByMilestoneId(milestoneId);
    }

    public List<Task> getTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId);
    }

    public List<Task> getTasksByResearchDecision(Long researchDecisionId) {
        return taskRepository.findByResearchDecisionId(researchDecisionId);
    }

    public List<Task> getTasksByExperiment(Long experimentId) {
        return taskRepository.findByExperimentId(experimentId);
    }

    public List<Task> getTasksByProject(Long projectId) {
        Map<Long, Task> tasks = new LinkedHashMap<>();

        List<Task> directTasks = taskRepository.findByProjectId(projectId);
        for (Task task : directTasks) {
            if (task.getId() != null) {
                tasks.put(task.getId(), task);
            }
        }

        List<Long> milestoneIds = milestoneService.getMilestonesByProject(projectId).stream()
            .map(Milestone::getId)
            .filter(Objects::nonNull)
            .toList();
        if (!milestoneIds.isEmpty()) {
            for (Task task : taskRepository.findByMilestoneIdIn(milestoneIds)) {
                if (task.getId() != null && !tasks.containsKey(task.getId())) {
                    tasks.put(task.getId(), task);
                }
            }
        }

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
                    if (task.getId() != null && !tasks.containsKey(task.getId())) {
                        tasks.put(task.getId(), task);
                    }
                });
        }
        return List.copyOf(tasks.values());
    }

    @Transactional
    public Task saveTask(Task task) {
        return saveTask(task, null);
    }

    @Transactional
    public Task saveTask(Task task, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        TaskStatus previousStatus = null;
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
                event.setTitle("Task Completed: " + saved.getTitle());
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

        return saved;
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

    public List<TaskHistory> getTaskHistory(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        if (taskHistoryRepository == null) {
            return List.of();
        }
        return taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(taskId);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
}
