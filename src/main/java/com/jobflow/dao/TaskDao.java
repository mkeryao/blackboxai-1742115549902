package com.jobflow.dao;

import com.jobflow.domain.Task;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskDao {
    /**
     * Save or update a task
     */
    Task save(Task task);

    /**
     * Find task by ID
     */
    Optional<Task> findById(Long id);

    /**
     * Find tasks by workflow ID
     */
    List<Task> findByWorkflowId(Long workflowId);

    /**
     * Find scheduled tasks that should be executed at the given time
     * This considers both the start_time and end_time of tasks
     */
    List<Task> findScheduledTasks(LocalDateTime now);
}
