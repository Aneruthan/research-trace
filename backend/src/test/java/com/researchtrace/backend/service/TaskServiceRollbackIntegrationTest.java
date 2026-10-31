package com.researchtrace.backend.service;

import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchDecisionStatus;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Task;
import com.researchtrace.backend.model.TaskHistory;
import com.researchtrace.backend.model.TaskStatus;
import com.researchtrace.backend.repository.ResearchDecisionRepository;
import com.researchtrace.backend.repository.ResearchEventRepository;
import com.researchtrace.backend.repository.TaskHistoryRepository;
import com.researchtrace.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@Import({TaskService.class, MilestoneService.class, ProgressService.class})
class TaskServiceRollbackIntegrationTest {

    private static final Long PROJECT_ID = 1L;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private ResearchDecisionRepository researchDecisionRepository;

    @MockitoBean
    private ResearchEventRepository researchEventRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackTaskStatusAndHistoryWhenResearchEventPersistenceFails() {
        ResearchDecision decision = new ResearchDecision();
        decision.setProjectId(PROJECT_ID);
        decision.setTitle("Decision for task");
        decision.setStatus(ResearchDecisionStatus.ACTIVE);
        decision = researchDecisionRepository.save(decision);

        Task task = new Task();
        task.setProjectId(PROJECT_ID);
        task.setTitle("Rollback test task");
        task.setStatus(TaskStatus.TODO);
        task.setResearchDecisionId(decision.getId());
        task = taskRepository.save(task);
        Long taskId = task.getId();

        // Simulate failure when saving research event
        doThrow(new RuntimeException("Simulated research event DB persistence failure"))
            .when(researchEventRepository).save(any(ResearchEvent.class));

        Task update = new Task();
        update.setId(taskId);
        update.setStatus(TaskStatus.COMPLETED);

        assertThatThrownBy(() -> taskService.saveTask(update, 55L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Simulated research event DB persistence failure");

        // Verify task status was NOT updated to COMPLETED in the database
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.TODO);

        // Verify no TaskHistory was saved in the database
        List<TaskHistory> history = taskHistoryRepository.findByTaskIdOrderByChangedAtAscIdAsc(taskId);
        assertThat(history).isEmpty();
    }
}
