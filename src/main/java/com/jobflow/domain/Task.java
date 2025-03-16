package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {
    
    public enum TaskType {
        HTTP,
        SHELL,
        SPRING_BEAN
    }

    public enum TaskStatus {
        CREATED,
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }

    private String name;
    private String description;
    private String groupName;
    private TaskType type;
    private TaskStatus status;
    
    // Task configuration
    private String command;         // For SHELL type
    private String url;            // For HTTP type
    private String beanName;       // For SPRING_BEAN type
    private String methodName;     // For SPRING_BEAN type
    private String params;         // JSON string of parameters
    
    // Schedule configuration
    private String cronExpression;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String workCalendar;   // JSON string of work calendar configuration
    
    // Retry configuration
    private Integer maxRetries;
    private Integer currentRetries;
    private Long retryInterval;    // in milliseconds
    private Long timeout;          // in milliseconds
    
    // Notification configuration
    private String notifyUsers;    // JSON array of user IDs
    private Boolean notifyOnSuccess;
    private Boolean notifyOnFailure;
    
    // Execution details
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private String lastExecutionResult;
    private Long lastExecutionDuration;  // in milliseconds
    
    // Task priority (lower number means higher priority)
    private Integer priority;
    
    // Task enabled status
    private Boolean enabled;

    public Task() {
        this.status = TaskStatus.CREATED;
        this.maxRetries = 3;
        this.currentRetries = 0;
        this.retryInterval = 300000L; // 5 minutes
        this.timeout = 3600000L;      // 1 hour
        this.notifyOnSuccess = false;
        this.notifyOnFailure = true;
        this.priority = 5;
        this.enabled = true;
    }

    public boolean isRetryable() {
        return this.currentRetries < this.maxRetries 
            && (this.status == TaskStatus.FAILED || this.status == TaskStatus.TIMEOUT);
    }

    public boolean isExecutable() {
        if (!this.enabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        
        if (this.startTime != null && now.isBefore(this.startTime)) {
            return false;
        }

        if (this.endTime != null && now.isAfter(this.endTime)) {
            return false;
        }

        return this.status != TaskStatus.RUNNING 
            && this.status != TaskStatus.PENDING;
    }

    public void incrementRetries() {
        this.currentRetries++;
        if (this.currentRetries >= this.maxRetries) {
            this.status = TaskStatus.FAILED;
        }
    }

    public void resetRetries() {
        this.currentRetries = 0;
    }

    public void markAsRunning() {
        this.status = TaskStatus.RUNNING;
        this.lastExecutionTime = LocalDateTime.now();
    }

    public void markAsCompleted(boolean success, String result, long duration) {
        this.status = success ? TaskStatus.SUCCESS : TaskStatus.FAILED;
        this.lastExecutionResult = result;
        this.lastExecutionDuration = duration;
        if (success) {
            this.resetRetries();
        }
    }

    public void markAsTimeout() {
        this.status = TaskStatus.TIMEOUT;
        this.lastExecutionResult = "Task execution timed out after " + this.timeout + "ms";
    }
}
