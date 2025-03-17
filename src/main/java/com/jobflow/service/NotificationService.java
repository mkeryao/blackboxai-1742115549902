package com.jobflow.service;

import com.jobflow.domain.NotificationConfig;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    /**
     * Send notifications for a task event
     */
    void sendTaskNotifications(Task task, NotificationConfig.NotificationType eventType, Map<String, Object> context);

    /**
     * Send notifications for a workflow event
     */
    void sendWorkflowNotifications(Workflow workflow, NotificationConfig.NotificationType eventType, Map<String, Object> context);

    /**
     * Send a single notification
     */
    void sendNotification(NotificationConfig config, String content, Map<String, Object> context);

    /**
     * Get notification history for a task
     */
    List<Map<String, Object>> getTaskNotificationHistory(Long taskId);

    /**
     * Get notification history for a workflow
     */
    List<Map<String, Object>> getWorkflowNotificationHistory(Long workflowId);

    /**
     * Get notification statistics
     */
    Map<String, Object> getNotificationStatistics(Long tenantId);

    /**
     * Test notification configuration
     */
    boolean testNotificationConfig(NotificationConfig config);

    /**
     * Get supported notification channels
     */
    List<NotificationConfig.NotificationChannel> getSupportedChannels();

    /**
     * Get notification templates
     */
    Map<String, String> getNotificationTemplates();

    /**
     * Validate notification template
     */
    boolean validateTemplate(String template);

    /**
     * Process notification template
     */
    String processTemplate(String template, Map<String, Object> context);

    /**
     * Get notification recipients
     */
    List<String> getNotificationRecipients(NotificationConfig config);

    /**
     * Validate notification recipient
     */
    boolean validateRecipient(String recipient, NotificationConfig.NotificationChannel channel);

    /**
     * Get failed notifications
     */
    List<Map<String, Object>> getFailedNotifications();

    /**
     * Retry failed notification
     */
    boolean retryFailedNotification(Long notificationId);

    /**
     * Get notification settings
     */
    Map<String, Object> getNotificationSettings();

    /**
     * Update notification settings
     */
    void updateNotificationSettings(Map<String, Object> settings);

    /**
     * Enable/disable notifications
     */
    void setNotificationsEnabled(boolean enabled);

    /**
     * Check if notifications are enabled
     */
    boolean areNotificationsEnabled();

    /**
     * Get notification rate limits
     */
    Map<String, Integer> getNotificationRateLimits();

    /**
     * Update notification rate limits
     */
    void updateNotificationRateLimits(Map<String, Integer> rateLimits);
}
