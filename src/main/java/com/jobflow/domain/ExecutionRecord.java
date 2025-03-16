package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fj_execution_record")
@EqualsAndHashCode(callSuper = true)
public class ExecutionRecord extends BaseEntity {

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "stack_trace")
    @Lob
    private String stackTrace;

    @Column(name = "input_params")
    @Lob
    private String inputParams;

    @Column(name = "output_result")
    @Lob
    private String outputResult;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @Column(name = "executor")
    private String executor;

    @Column(name = "executor_ip")
    private String executorIp;

    @Column(name = "trigger_type")
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    @Column(name = "trigger_info")
    private String triggerInfo;

    @Column(name = "environment")
    private String environment;

    @Column(name = "resource_usage")
    private String resourceUsage;

    public enum ExecutionType {
        TASK,
        WORKFLOW
    }

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT,
        RETRY
    }

    public enum TriggerType {
        MANUAL,
        SCHEDULED,
        API,
        WORKFLOW,
        EVENT
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        if (endTime != null && startTime != null) {
            duration = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    public void markAsStarted() {
        this.status = ExecutionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    public void markAsCompleted(String result) {
        this.status = ExecutionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.outputResult = result;
        calculateDuration();
    }

    public void markAsFailed(String errorMessage, String stackTrace) {
        this.status = ExecutionStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        calculateDuration();
    }

    public void markAsRetry(LocalDateTime nextRetryTime) {
        this.status = ExecutionStatus.RETRY;
        this.retryCount++;
        this.nextRetryTime = nextRetryTime;
    }

    public void markAsCancelled(String reason) {
        this.status = ExecutionStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = reason;
        calculateDuration();
    }

    public void markAsTimeout() {
        this.status = ExecutionStatus.TIMEOUT;
        this.endTime = LocalDateTime.now();
        this.errorMessage = "Execution timed out";
        calculateDuration();
    }

    private void calculateDuration() {
        if (endTime != null && startTime != null) {
            this.duration = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
}
