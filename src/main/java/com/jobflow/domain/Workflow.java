package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Workflow extends BaseEntity {
    
    public enum WorkflowStatus {
        CREATED,
        SCHEDULED,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private String name;
    private String description;
    private WorkflowStatus status;
    
    // Schedule configuration
    private String cronExpression;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String workCalendar;   // JSON string of work calendar configuration
    
    // Notification configuration
    private String notifyUsers;    // JSON array of user IDs
    private Boolean notifyOnSuccess;
    private Boolean notifyOnFailure;
    
    // Execution details
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private String lastExecutionResult;
    private Long lastExecutionDuration;  // in milliseconds
    
    // DAG configuration
    private String dagDefinition;  // JSON string representing the DAG structure
    
    // Workflow enabled status
    private Boolean enabled;
    
    // Timeout configuration
    private Long timeout;          // in milliseconds
    
    // Current execution progress
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer failedTasks;
    
    // Transient fields (not stored in database)
    private transient List<Task> tasks;
    private transient List<WorkflowDependency> dependencies;

    public Workflow() {
        this.status = WorkflowStatus.CREATED;
        this.notifyOnSuccess = false;
        this.notifyOnFailure = true;
        this.enabled = true;
        this.timeout = 86400000L;  // 24 hours
        this.totalTasks = 0;
        this.completedTasks = 0;
        this.failedTasks = 0;
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

        return this.status != WorkflowStatus.RUNNING;
    }

    public void markAsRunning() {
        this.status = WorkflowStatus.RUNNING;
        this.lastExecutionTime = LocalDateTime.now();
        this.completedTasks = 0;
        this.failedTasks = 0;
    }

    public void updateProgress(boolean taskSuccess) {
        this.completedTasks++;
        if (!taskSuccess) {
            this.failedTasks++;
        }
        
        if (this.completedTasks.equals(this.totalTasks)) {
            this.status = this.failedTasks == 0 ? WorkflowStatus.SUCCESS : WorkflowStatus.FAILED;
        }
    }

    public void markAsCompleted(boolean success, String result, long duration) {
        this.status = success ? WorkflowStatus.SUCCESS : WorkflowStatus.FAILED;
        this.lastExecutionResult = result;
        this.lastExecutionDuration = duration;
    }

    public double getProgress() {
        return this.totalTasks == 0 ? 0 : 
            (double) this.completedTasks / this.totalTasks * 100;
    }

    public boolean isCompleted() {
        return this.status == WorkflowStatus.SUCCESS || 
               this.status == WorkflowStatus.FAILED;
    }
}
