package com.jobflow.service.impl;

import com.jobflow.dao.ExecutionRecordDao;
import com.jobflow.domain.ExecutionRecord;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import com.jobflow.service.ExecutionRecordService;
import com.jobflow.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ExecutionRecordServiceImpl extends AbstractBaseService<ExecutionRecord> implements ExecutionRecordService {

    private final ExecutionRecordDao executionRecordDao;
    private final NotificationService notificationService;

    @Autowired
    public ExecutionRecordServiceImpl(ExecutionRecordDao executionRecordDao, NotificationService notificationService) {
        super(executionRecordDao);
        this.executionRecordDao = executionRecordDao;
        this.notificationService = notificationService;
    }

    @Override
    public ExecutionRecord createTaskExecution(Task task, String executor, String executorIp,
                                             ExecutionRecord.TriggerType triggerType, String triggerInfo) {
        ExecutionRecord record = new ExecutionRecord();
        record.setExecutionId(UUID.randomUUID().toString());
        record.setType(ExecutionRecord.ExecutionType.TASK);
        record.setTask(task);
        record.setExecutor(executor);
        record.setExecutorIp(executorIp);
        record.setTriggerType(triggerType);
        record.setTriggerInfo(triggerInfo);
        record.setStatus(ExecutionRecord.ExecutionStatus.PENDING);
        record.setTenantId(getCurrentTenantId());
        return save(record);
    }

    @Override
    public ExecutionRecord createWorkflowExecution(Workflow workflow, String executor, String executorIp,
                                                 ExecutionRecord.TriggerType triggerType, String triggerInfo) {
        ExecutionRecord record = new ExecutionRecord();
        record.setExecutionId(UUID.randomUUID().toString());
        record.setType(ExecutionRecord.ExecutionType.WORKFLOW);
        record.setWorkflow(workflow);
        record.setExecutor(executor);
        record.setExecutorIp(executorIp);
        record.setTriggerType(triggerType);
        record.setTriggerInfo(triggerInfo);
        record.setStatus(ExecutionRecord.ExecutionStatus.PENDING);
        record.setTenantId(getCurrentTenantId());
        return save(record);
    }

    @Override
    public List<ExecutionRecord> getTaskExecutions(Long taskId) {
        return executionRecordDao.findTaskExecutions(getCurrentTenantId(), 
                                                   ExecutionRecord.ExecutionType.TASK, taskId);
    }

    @Override
    public List<ExecutionRecord> getWorkflowExecutions(Long workflowId) {
        return executionRecordDao.findWorkflowExecutions(getCurrentTenantId(), 
                                                       ExecutionRecord.ExecutionType.WORKFLOW, workflowId);
    }

    @Override
    public List<ExecutionRecord> findRetryableExecutions() {
        return executionRecordDao.findRetryableExecutions(getCurrentTenantId(), 
                                                        ExecutionRecord.ExecutionStatus.RETRY, 
                                                        LocalDateTime.now());
    }

    @Override
    public List<ExecutionRecord> findByStatusAndStartTime(List<ExecutionRecord.ExecutionStatus> statuses, 
                                                        LocalDateTime startTime) {
        return executionRecordDao.findByStatusAndStartTime(getCurrentTenantId(), statuses, startTime);
    }

    @Override
    public Map<ExecutionRecord.ExecutionStatus, Long> getExecutionStatistics(LocalDateTime start, LocalDateTime end) {
        List<Object[]> stats = executionRecordDao.getExecutionStatistics(getCurrentTenantId(), start, end);
        return stats.stream().collect(Collectors.toMap(
            row -> (ExecutionRecord.ExecutionStatus) row[0],
            row -> (Long) row[1]
        ));
    }

    @Override
    public Double getAverageExecutionTime(ExecutionRecord.ExecutionType type, LocalDateTime start, LocalDateTime end) {
        return executionRecordDao.getAverageExecutionTime(getCurrentTenantId(), type, start, end);
    }

    @Override
    public List<ExecutionRecord> findTimedOutExecutions(int timeoutMinutes) {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return executionRecordDao.findTimedOutExecutions(getCurrentTenantId(), timeout);
    }

    @Override
    public ExecutionRecord getByExecutionId(String executionId) {
        return executionRecordDao.findByExecutionId(getCurrentTenantId(), executionId);
    }

    @Override
    public Page<ExecutionRecord> searchExecutions(ExecutionRecord.ExecutionType type,
                                                ExecutionRecord.ExecutionStatus status,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                Long resourceId,
                                                Pageable pageable) {
        return executionRecordDao.searchExecutions(getCurrentTenantId(), type, status, 
                                                 startTime, endTime, resourceId, pageable);
    }

    @Override
    public List<Map<String, Object>> getDetailedStatistics(LocalDateTime start, LocalDateTime end) {
        return executionRecordDao.getDetailedStatistics(getCurrentTenantId(), start, end);
    }

    @Override
    public boolean hasRunningExecution(ExecutionRecord.ExecutionType type, Long taskId, Long workflowId) {
        return executionRecordDao.hasRunningExecution(getCurrentTenantId(), type, taskId, workflowId);
    }

    @Override
    public ExecutionRecord startExecution(String executionId) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsStarted();
        return save(record);
    }

    @Override
    public ExecutionRecord completeExecution(String executionId, String result) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsCompleted(result);
        record = save(record);
        notificationService.sendExecutionCompletedNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord failExecution(String executionId, String errorMessage, String stackTrace) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsFailed(errorMessage, stackTrace);
        record = save(record);
        notificationService.sendExecutionFailedNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord retryExecution(String executionId, LocalDateTime nextRetryTime) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsRetry(nextRetryTime);
        return save(record);
    }

    @Override
    public ExecutionRecord cancelExecution(String executionId, String reason) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsCancelled(reason);
        record = save(record);
        notificationService.sendExecutionCancelledNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord timeoutExecution(String executionId) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsTimeout();
        record = save(record);
        notificationService.sendExecutionTimeoutNotification(record);
        return record;
    }

    @Override
    public double getSuccessRate(ExecutionRecord.ExecutionType type, LocalDateTime start, LocalDateTime end) {
        Map<ExecutionRecord.ExecutionStatus, Long> stats = getExecutionStatistics(start, end);
        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        long successful = stats.getOrDefault(ExecutionRecord.ExecutionStatus.COMPLETED, 0L);
        return total > 0 ? (double) successful / total : 0.0;
    }

    @Override
    public Map<String, Object> getExecutionTrend(ExecutionRecord.ExecutionType type,
                                               LocalDateTime start,
                                               LocalDateTime end,
                                               String interval) {
        // Implementation depends on specific trend analysis requirements
        Map<String, Object> trend = new HashMap<>();
        // Add trend data based on interval (hourly, daily, weekly, etc.)
        return trend;
    }

    @Override
    public Map<String, Object> getResourceExecutionSummary(ExecutionRecord.ExecutionType type, 
                                                          Long resourceId,
                                                          LocalDateTime start,
                                                          LocalDateTime end) {
        Map<String, Object> summary = new HashMap<>();
        List<ExecutionRecord> executions;
        
        if (type == ExecutionRecord.ExecutionType.TASK) {
            executions = getTaskExecutions(resourceId);
        } else {
            executions = getWorkflowExecutions(resourceId);
        }
        
        executions = executions.stream()
            .filter(e -> e.getStartTime().isAfter(start) && e.getStartTime().isBefore(end))
            .collect(Collectors.toList());

        summary.put("totalExecutions", executions.size());
        summary.put("successRate", calculateSuccessRate(executions));
        summary.put("averageDuration", calculateAverageDuration(executions));
        summary.put("lastExecution", executions.isEmpty() ? null : executions.get(executions.size() - 1));
        
        return summary;
    }

    @Override
    public void cleanupOldRecords(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        // Implement cleanup logic
    }

    @Override
    public byte[] exportExecutionRecords(LocalDateTime start, LocalDateTime end, String format) {
        // Implement export logic based on format (CSV, Excel, etc.)
        return new byte[0];
    }

    @Override
    public Map<String, Object> getExecutionMetrics(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Add various metrics
        metrics.put("totalExecutions", getExecutionStatistics(start, end));
        metrics.put("averageExecutionTime", getAverageExecutionTime(null, start, end));
        metrics.put("successRate", getSuccessRate(null, start, end));
        
        return metrics;
    }

    private double calculateSuccessRate(List<ExecutionRecord> executions) {
        if (executions.isEmpty()) return 0.0;
        long successful = executions.stream()
            .filter(e -> e.getStatus() == ExecutionRecord.ExecutionStatus.COMPLETED)
            .count();
        return (double) successful / executions.size();
    }

    private double calculateAverageDuration(List<ExecutionRecord> executions) {
        return executions.stream()
            .filter(e -> e.getDuration() != null)
            .mapToLong(ExecutionRecord::getDuration)
            .average()
            .orElse(0.0);
    }
}
