package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
    
    public enum NotificationType {
        EMAIL,
        WECHAT,
        WEBHOOK
    }

    public enum NotificationStatus {
        PENDING,
        SENDING,
        SENT,
        FAILED,
        CANCELLED
    }

    public enum NotificationLevel {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public enum NotificationSource {
        TASK,
        WORKFLOW,
        SYSTEM
    }

    private String title;
    private String content;
    private NotificationType type;
    private NotificationStatus status;
    private NotificationLevel level;
    private NotificationSource source;
    
    // Source reference
    private Long sourceId;          // Task ID or Workflow ID
    private String sourceName;      // Task name or Workflow name
    
    // Recipient information
    private Long userId;
    private String recipient;       // Email address or WeChat ID or Webhook URL
    
    // Retry configuration
    private Integer maxRetries;
    private Integer currentRetries;
    private Long retryInterval;     // in milliseconds
    
    // Sending details
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private String errorMessage;
    private String responseData;    // Response from notification service
    
    // Additional parameters (stored as JSON)
    private String parameters;

    public Notification() {
        this.status = NotificationStatus.PENDING;
        this.level = NotificationLevel.INFO;
        this.maxRetries = 3;
        this.currentRetries = 0;
        this.retryInterval = 300000L; // 5 minutes
        this.scheduledTime = LocalDateTime.now();
    }

    /**
     * Check if notification can be retried
     */
    public boolean isRetryable() {
        return this.status == NotificationStatus.FAILED 
            && this.currentRetries < this.maxRetries;
    }

    /**
     * Increment retry count
     */
    public void incrementRetries() {
        this.currentRetries++;
        if (this.currentRetries >= this.maxRetries) {
            this.status = NotificationStatus.FAILED;
        }
    }

    /**
     * Mark notification as sent
     */
    public void markAsSent(String response) {
        this.status = NotificationStatus.SENT;
        this.sentTime = LocalDateTime.now();
        this.responseData = response;
    }

    /**
     * Mark notification as failed
     */
    public void markAsFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error;
    }

    /**
     * Get next retry time
     */
    public LocalDateTime getNextRetryTime() {
        if (!isRetryable()) {
            return null;
        }
        return LocalDateTime.now().plusNanos(retryInterval * 1000000); // Convert ms to nanos
    }

    /**
     * Check if notification is expired
     * Default expiration: 24 hours
     */
    public boolean isExpired() {
        return this.scheduledTime.plusHours(24).isBefore(LocalDateTime.now());
    }

    /**
     * Create a task notification
     */
    public static Notification createTaskNotification(Task task, User user, 
            NotificationType type, NotificationLevel level, String content) {
        Notification notification = new Notification();
        notification.setSource(NotificationSource.TASK);
        notification.setSourceId(task.getId());
        notification.setSourceName(task.getName());
        notification.setUserId(user.getId());
        notification.setType(type);
        notification.setLevel(level);
        notification.setTitle("Task Notification: " + task.getName());
        notification.setContent(content);
        
        switch (type) {
            case EMAIL:
                notification.setRecipient(user.getEmail());
                break;
            case WECHAT:
                notification.setRecipient(user.getWechatId());
                break;
            case WEBHOOK:
                // Webhook URL should be configured in user preferences
                notification.setRecipient(user.getPreferences()); // Assuming webhook URL is stored here
                break;
        }
        
        return notification;
    }

    /**
     * Create a workflow notification
     */
    public static Notification createWorkflowNotification(Workflow workflow, User user, 
            NotificationType type, NotificationLevel level, String content) {
        Notification notification = new Notification();
        notification.setSource(NotificationSource.WORKFLOW);
        notification.setSourceId(workflow.getId());
        notification.setSourceName(workflow.getName());
        notification.setUserId(user.getId());
        notification.setType(type);
        notification.setLevel(level);
        notification.setTitle("Workflow Notification: " + workflow.getName());
        notification.setContent(content);
        
        switch (type) {
            case EMAIL:
                notification.setRecipient(user.getEmail());
                break;
            case WECHAT:
                notification.setRecipient(user.getWechatId());
                break;
            case WEBHOOK:
                notification.setRecipient(user.getPreferences()); // Assuming webhook URL is stored here
                break;
        }
        
        return notification;
    }
}
