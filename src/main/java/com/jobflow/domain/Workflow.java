package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Workflow extends BaseEntity {
    private String name;
    private String description;
    private String cron;
    private WorkflowStatus status;
    private WorkflowPriority priority;
    private LocalDateTime startTime;  // Workflow scheduled start time
    private LocalDateTime endTime;    // Workflow scheduled end time
    private Integer timeout;
    private Integer retries;
    private Integer retryDelay;
    private String notification;
    private List<WorkflowDependency> dependencies;
    private String parameters;
    private Boolean concurrent;
    private String errorHandling;

    public enum WorkflowStatus {
        PENDING,    // Workflow is created but not yet scheduled
        SCHEDULED,  // Workflow is scheduled to run
        RUNNING,    // Workflow is currently running
        COMPLETED,  // Workflow completed successfully
        FAILED,     // Workflow failed
        CANCELLED,  // Workflow was cancelled
        TIMEOUT,    // Workflow timed out
        RETRY,      // Workflow is waiting for retry
        PAUSED      // Workflow is paused
    }

    public enum WorkflowPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Validate workflow schedule
    public boolean isValidSchedule() {
        if (startTime == null) {
            return true; // No schedule restrictions
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            return false; // End time must be after start time
        }
        return !startTime.isBefore(LocalDateTime.now());
    }

    // Check if workflow is within its scheduled time window
    public boolean isWithinSchedule(LocalDateTime now) {
        if (startTime == null) {
            return true; // No schedule restrictions
        }
        if (now.isBefore(startTime)) {
            return false; // Too early
        }
        return endTime == null || !now.isAfter(endTime);
    }

    // Check if workflow has expired
    public boolean hasExpired(LocalDateTime now) {
        return endTime != null && now.isAfter(endTime);
    }

    // Calculate remaining time before workflow expires
    public Long getRemainingTime(LocalDateTime now) {
        if (endTime == null) {
            return null; // No expiration
        }
        return java.time.Duration.between(now, endTime).toSeconds();
    }

    // Check if workflow can be retried
    public boolean canRetry() {
        return retries != null && retries > 0;
    }

    // Get next retry time
    public LocalDateTime getNextRetryTime(LocalDateTime lastAttempt) {
        if (!canRetry()) {
            return null;
        }
        return lastAttempt.plusSeconds(retryDelay != null ? retryDelay : 60);
    }

    // Check if workflow is critical
    public boolean isCritical() {
        return WorkflowPriority.CRITICAL.equals(priority);
    }

    // Check if workflow needs immediate attention
    public boolean needsAttention() {
        return WorkflowStatus.FAILED.equals(status) || 
               WorkflowStatus.TIMEOUT.equals(status) ||
               (WorkflowStatus.RETRY.equals(status) && !canRetry());
    }

    // Check if workflow allows concurrent execution
    public boolean allowsConcurrent() {
        return Boolean.TRUE.equals(concurrent);
    }

    // Check if workflow has dependencies
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }

    // Check if workflow dependencies are met
    public boolean areDependenciesMet(List<Workflow> completedWorkflows) {
        if (!hasDependencies()) {
            return true;
        }
        
        return dependencies.stream().allMatch(dependency -> {
            return completedWorkflows.stream()
                .anyMatch(workflow -> workflow.getId().equals(dependency.getDependencyId()) &&
                         WorkflowStatus.COMPLETED.equals(workflow.getStatus()));
        });
    }

    // Get estimated duration based on historical executions
    public Long getEstimatedDuration() {
        // This would be implemented based on historical execution records
        return null;
    }

    // Check if workflow can be paused
    public boolean canBePaused() {
        return WorkflowStatus.RUNNING.equals(status);
    }

    // Check if workflow can be resumed
    public boolean canBeResumed() {
        return WorkflowStatus.PAUSED.equals(status);
    }

    // Check if workflow should be auto-retried
    public boolean shouldAutoRetry() {
        return "AUTO_RETRY".equals(errorHandling) && canRetry();
    }

    // Check if workflow should skip failed tasks
    public boolean shouldSkipFailedTasks() {
        return "SKIP_FAILED".equals(errorHandling);
    }

    // Check if workflow should stop on first failure
    public boolean shouldStopOnFailure() {
        return "STOP_ON_FAILURE".equals(errorHandling);
    }
}
