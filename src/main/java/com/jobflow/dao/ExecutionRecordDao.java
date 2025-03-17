package com.jobflow.dao;

import com.jobflow.domain.ExecutionRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExecutionRecordDao {

    /**
     * Save or update execution record
     */
    ExecutionRecord save(ExecutionRecord record);

    /**
     * Find execution record by ID
     */
    Optional<ExecutionRecord> findById(Long id);

    /**
     * Find task executions
     */
    List<ExecutionRecord> findTaskExecutions(Long tenantId, ExecutionRecord.ExecutionType type, Long taskId);

    /**
     * Find workflow executions
     */
    List<ExecutionRecord> findWorkflowExecutions(Long tenantId, ExecutionRecord.ExecutionType type, Long workflowId);

    /**
     * Find retryable executions
     */
    List<ExecutionRecord> findRetryableExecutions(Long tenantId, ExecutionRecord.ExecutionStatus status, LocalDateTime now);

    /**
     * Find executions by status and start time
     */
    List<ExecutionRecord> findByStatusAndStartTime(Long tenantId, List<ExecutionRecord.ExecutionStatus> statuses, LocalDateTime startTime);

    /**
     * Get execution statistics
     */
    List<Object[]> getExecutionStatistics(Long tenantId, LocalDateTime start, LocalDateTime end);

    /**
     * Get average execution time
     */
    Double getAverageExecutionTime(Long tenantId, ExecutionRecord.ExecutionType type, LocalDateTime start, LocalDateTime end);

    /**
     * Find timed out executions
     */
    List<ExecutionRecord> findTimedOutExecutions(Long tenantId, LocalDateTime timeout);

    /**
     * Find execution by execution ID
     */
    ExecutionRecord findByExecutionId(Long tenantId, String executionId);
}
