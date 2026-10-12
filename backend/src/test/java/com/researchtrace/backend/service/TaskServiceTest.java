package com.researchtrace.backend.service;

import com.researchtrace.backend.model.Milestone;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskSuggestion;
import com.researchtrace.backend.model.TaskSuggestionStatus;
import com.researchtrace.backend.repository.TaskRepository;
import com.researchtrace.backend.repository.TaskSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private TaskSuggestionRepository taskSuggestionRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, milestoneService, taskSuggestionRepository);
    }

    @Test
    void returnsTasksOnTheProjectsMilestones() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone(11L), milestone(12L)));
        Task first = task(500L, 11L);
        Task second = task(501L, 12L);
        when(taskRepository.findByMilestoneIdIn(List.of(11L, 12L))).thenReturn(List.of(first, second));
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of());

        assertThat(taskService.getTasksByProject(PROJECT_ID)).containsExactly(first, second);
    }

    @Test
    void includesAcceptedTasksThatHaveNoMilestone() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of(milestone(11L)));
        Task onMilestone = task(500L, 11L);
        Task acceptedWithoutMilestone = task(501L, null);
        when(taskRepository.findByMilestoneIdIn(List.of(11L))).thenReturn(List.of(onMilestone));
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of(acceptedSuggestion(500L), acceptedSuggestion(501L)));
        when(taskRepository.findAllById(List.of(501L))).thenReturn(List.of(acceptedWithoutMilestone));

        assertThat(taskService.getTasksByProject(PROJECT_ID))
            .containsExactly(onMilestone, acceptedWithoutMilestone);
    }

    @Test
    void doesNotQueryMilestonesWhenTheProjectHasNone() {
        when(milestoneService.getMilestonesByProject(PROJECT_ID)).thenReturn(List.of());
        when(taskSuggestionRepository.findByProjectIdAndStatus(PROJECT_ID, TaskSuggestionStatus.ACCEPTED))
            .thenReturn(List.of());

        assertThat(taskService.getTasksByProject(PROJECT_ID)).isEmpty();
        verify(taskRepository, never()).findByMilestoneIdIn(List.of());
    }

    private static Milestone milestone(Long id) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProjectId(PROJECT_ID);
        return milestone;
    }

    private static Task task(Long id, Long milestoneId) {
        Task task = new Task();
        task.setId(id);
        task.setMilestoneId(milestoneId);
        task.setTitle("Task " + id);
        return task;
    }

    private static TaskSuggestion acceptedSuggestion(Long taskId) {
        TaskSuggestion suggestion = new TaskSuggestion();
        suggestion.setProjectId(PROJECT_ID);
        suggestion.setStatus(TaskSuggestionStatus.ACCEPTED);
        suggestion.setAcceptedTaskId(taskId);
        return suggestion;
    }
}
