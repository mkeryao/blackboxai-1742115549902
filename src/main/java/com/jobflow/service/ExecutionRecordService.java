package com.jobflow.service;

import com.jobflow.domain.ExecutionRecord;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ExecutionRecordService extends BaseService<ExecutionRecord> {

    ExecutionRecord createTaskExecution(Task task, String executor, String executorIp, 
                                      ExecutionRecord.TriggerType triggerType, String triggerInfo);

    ExecutionRecord createWorkflowExecution(Workflow workflow, String executor, String executorIp, 
                                          ExecutionRecord.TriggerType triggerType, String triggerInfo);

    List<ExecutionRecord> getTaskExecutions(Long taskId);

    List<ExecutionRecord> getWorkflowExecutions(Long workflowId);

    List<ExecutionRecord> findRetryableExecutions();

    List<ExecutionRecord> findByStatusAndStartTime(List<ExecutionRecord.ExecutionStatus> statuses, 
                                                 LocalDateTime startTime);

    Map<ExecutionRecord.ExecutionStatus, Long> getExecutionStatistics(LocalDateTime start, 
                                                                     LocalDateTime end);

    Double getAverageExecutionTime(ExecutionRecord.ExecutionType type, LocalDateTime start, 
                                 LocalDateTime end);

    List<ExecutionRecord> findTimedOutExecutions(int timeoutMinutes);

    ExecutionRecord getByExecutionId(String executionId);

    Page<ExecutionRecord> searchExecutions(ExecutionRecord.ExecutionType type,
                                         ExecutionRecord.ExecutionStatus status,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime,
                                         Long resourceId,
                                         Pageable pageable);

    List<Map<String, Object>> getDetailedStatistics(LocalDateTime start, LocalDateTime end);

    boolean hasRunningExecution(ExecutionRecord.ExecutionType type, Long taskId, Long workflowId);

    ExecutionRecord startExecution(String executionId);

    ExecutionRecord completeExecution(String executionId, String result);

    ExecutionRecord failExecution(String executionId, String errorMessage, String stackTrace);

    ExecutionRecord retryExecution(String executionId, LocalDateTime nextRetryTime);

    ExecutionRecord cancelExecution(String executionId, String reason);

    ExecutionRecord timeoutExecution(String executionId);

    double getSuccessRate(ExecutionRecord.ExecutionType type, LocalDateTime start, LocalDateTime end);

    Map<String, Object> getExecutionTrend(ExecutionRecord.ExecutionType type, 
                                        LocalDateTime start, 
                                        LocalDateTime end,
                                        String interval);

    Map<String, Object> getResourceExecutionSummary(ExecutionRecord.ExecutionType type, 
                                                   Long resourceId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    void cleanupOldRecords(int retentionDays);

    byte[] exportExecutionRecords(LocalDateTime start, LocalDateTime end, String format);

    Map<String, Object> getExecutionMetrics(LocalDateTime start, LocalDateTime end);
}
