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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ExecutionRecordServiceImpl implements ExecutionRecordService {

    private final ExecutionRecordDao executionRecordDao;
    private final NotificationService notificationService;

    @Autowired
    public ExecutionRecordServiceImpl(ExecutionRecordDao executionRecordDao, NotificationService notificationService) {
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
        return executionRecordDao.save(record);
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
        return executionRecordDao.save(record);
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
        // Note: Implement pagination in DAO layer if needed
        List<ExecutionRecord> records = findByStatusAndStartTime(
            status != null ? Collections.singletonList(status) : Arrays.asList(ExecutionRecord.ExecutionStatus.values()),
            startTime != null ? startTime : LocalDateTime.now().minusDays(30)
        );

        // Filter by type and resourceId if provided
        if (type != null) {
            records = records.stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
        }

        if (resourceId != null) {
            records = records.stream()
                .filter(r -> (r.getType() == ExecutionRecord.ExecutionType.TASK && r.getTask() != null && r.getTask().getId().equals(resourceId)) ||
                           (r.getType() == ExecutionRecord.ExecutionType.WORKFLOW && r.getWorkflow() != null && r.getWorkflow().getId().equals(resourceId)))
                .collect(Collectors.toList());
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), records.size());
        return new PageImpl<>(records.subList(start, end), pageable, records.size());
    }

    @Override
    public List<Map<String, Object>> getDetailedStatistics(LocalDateTime start, LocalDateTime end) {
        Map<ExecutionRecord.ExecutionStatus, Long> stats = getExecutionStatistics(start, end);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<ExecutionRecord.ExecutionStatus, Long> entry : stats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }

        return result;
    }

    @Override
    public boolean hasRunningExecution(ExecutionRecord.ExecutionType type, Long taskId, Long workflowId) {
        List<ExecutionRecord> runningExecutions = findByStatusAndStartTime(
            Collections.singletonList(ExecutionRecord.ExecutionStatus.RUNNING),
            LocalDateTime.now().minusDays(1)
        );

        return runningExecutions.stream()
            .anyMatch(r -> r.getType() == type &&
                ((type == ExecutionRecord.ExecutionType.TASK && r.getTask() != null && r.getTask().getId().equals(taskId)) ||
                 (type == ExecutionRecord.ExecutionType.WORKFLOW && r.getWorkflow() != null && r.getWorkflow().getId().equals(workflowId))));
    }

    @Override
    public ExecutionRecord startExecution(String executionId) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsStarted();
        return executionRecordDao.save(record);
    }

    @Override
    public ExecutionRecord completeExecution(String executionId, String result) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsCompleted(result);
        record = executionRecordDao.save(record);
        notificationService.sendExecutionCompletedNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord failExecution(String executionId, String errorMessage, String stackTrace) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsFailed(errorMessage, stackTrace);
        record = executionRecordDao.save(record);
        notificationService.sendExecutionFailedNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord retryExecution(String executionId, LocalDateTime nextRetryTime) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsRetry(nextRetryTime);
        return executionRecordDao.save(record);
    }

    @Override
    public ExecutionRecord cancelExecution(String executionId, String reason) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsCancelled(reason);
        record = executionRecordDao.save(record);
        notificationService.sendExecutionCancelledNotification(record);
        return record;
    }

    @Override
    public ExecutionRecord timeoutExecution(String executionId) {
        ExecutionRecord record = getByExecutionId(executionId);
        record.markAsTimeout();
        record = executionRecordDao.save(record);
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
        Map<String, Object> trend = new HashMap<>();
        List<ExecutionRecord> records = findByStatusAndStartTime(
            Arrays.asList(ExecutionRecord.ExecutionStatus.values()),
            start
        );

        // Filter by type if provided
        if (type != null) {
            records = records.stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
        }

        // Group by date and status
        Map<LocalDateTime, Map<ExecutionRecord.ExecutionStatus, Long>> groupedRecords = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getStartTime().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
                Collectors.groupingBy(
                    ExecutionRecord::getStatus,
                    Collectors.counting()
                )
            ));

        trend.put("data", groupedRecords);
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
        // Implement cleanup logic if needed
    }

    @Override
    public byte[] exportExecutionRecords(LocalDateTime start, LocalDateTime end, String format) {
        // Implement export logic if needed
        return new byte[0];
    }

    @Override
    public Map<String, Object> getExecutionMetrics(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> metrics = new HashMap<>();
        Map<ExecutionRecord.ExecutionStatus, Long> stats = getExecutionStatistics(start, end);

        metrics.put("totalExecutions", stats.values().stream().mapToLong(Long::longValue).sum());
        metrics.put("successRate", getSuccessRate(null, start, end));
        metrics.put("averageExecutionTime", getAverageExecutionTime(null, start, end));
        metrics.put("failedExecutions", stats.getOrDefault(ExecutionRecord.ExecutionStatus.FAILED, 0L));

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

    private Long getCurrentTenantId() {
        // Implement based on your tenant management system
        return 1L;
    }
}
