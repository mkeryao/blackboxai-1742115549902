package com.jobflow.dao;

import com.jobflow.domain.Workflow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkflowDao {
    /**
     * Save or update a workflow
     */
    Workflow save(Workflow workflow);

    /**
     * Find workflow by ID
     */
    Optional<Workflow> findById(Long id);

    /**
     * Find scheduled workflows that should be executed at the given time
     * This considers both the start_time and end_time of workflows
     */
    List<Workflow> findScheduledWorkflows(LocalDateTime now);

    /**
     * Find workflows that depend on the given workflow
     * Used for managing workflow dependencies and scheduling
     */
    List<Workflow> findDependentWorkflows(Long workflowId);
}
