package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {
    private String name;
    private String description;
    private String command;
    private String cron;
    private Integer timeout;
    private Integer retries;
    private Integer retryDelay;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long workflowId;
    private Integer sequence;
    private String parameters;
    private List<NotificationConfig> notifications;  // Multiple notification configurations

    public enum TaskStatus {
        PENDING,    // Task is created but not yet scheduled
        SCHEDULED,  // Task is scheduled to run
        RUNNING,    // Task is currently running
        COMPLETED,  // Task completed successfully
        FAILED,     // Task failed
        CANCELLED,  // Task was cancelled
        TIMEOUT,    // Task timed out
        RETRY      // Task is waiting for retry
    }

    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Validate task schedule
    public boolean isValidSchedule() {
        if (startTime == null) {
            return true; // No schedule restrictions
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            return false; // End time must be after start time
        }
        return !startTime.isBefore(LocalDateTime.now());
    }

    // Check if task is within its scheduled time window
    public boolean isWithinSchedule(LocalDateTime now) {
        if (startTime == null) {
            return true; // No schedule restrictions
        }
        if (now.isBefore(startTime)) {
            return false; // Too early
        }
        return endTime == null || !now.isAfter(endTime);
    }

    // Check if task has expired
    public boolean hasExpired(LocalDateTime now) {
        return endTime != null && now.isAfter(endTime);
    }

    // Calculate remaining time before task expires
    public Long getRemainingTime(LocalDateTime now) {
        if (endTime == null) {
            return null; // No expiration
        }
        return java.time.Duration.between(now, endTime).toSeconds();
    }

    // Check if task can be retried
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

    // Check if task is critical
    public boolean isCritical() {
        return TaskPriority.CRITICAL.equals(priority);
    }

    // Check if task needs immediate attention
    public boolean needsAttention() {
        return TaskStatus.FAILED.equals(status) || 
               TaskStatus.TIMEOUT.equals(status) ||
               (TaskStatus.RETRY.equals(status) && !canRetry());
    }

    // Get notifications for a specific event type
    public List<NotificationConfig> getNotificationsForEvent(NotificationConfig.NotificationType type) {
        if (notifications == null) {
            return List.of();
        }
        return notifications.stream()
            .filter(config -> config.isEnabled() && config.shouldNotifyOn(type))
            .toList();
    }

    // Add a notification configuration
    public void addNotification(NotificationConfig config) {
        if (notifications == null) {
            notifications = new java.util.ArrayList<>();
        }
        notifications.add(config);
    }

    // Remove a notification configuration
    public void removeNotification(NotificationConfig config) {
        if (notifications != null) {
            notifications.remove(config);
        }
    }

    // Clear all notifications
    public void clearNotifications() {
        if (notifications != null) {
            notifications.clear();
        }
    }
}
