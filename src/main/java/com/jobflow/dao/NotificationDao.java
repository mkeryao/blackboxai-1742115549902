package com.jobflow.dao;

import com.jobflow.domain.NotificationConfig;
import java.util.List;
import java.util.Map;

public interface NotificationDao {
    /**
     * Save a failed notification
     */
    void saveFailedNotification(String type, Long sourceId, NotificationConfig.NotificationType eventType,
                              NotificationConfig config, String errorMessage);

    /**
     * Get notification history for a specific source (task or workflow)
     */
    List<Map<String, Object>> getNotificationHistory(String type, Long sourceId);

    /**
     * Get notification statistics for a tenant
     */
    Map<String, Object> getNotificationStatistics(Long tenantId);

    /**
     * Get list of failed notifications
     */
    List<Map<String, Object>> getFailedNotifications();

    /**
     * Retry a failed notification
     */
    boolean retryFailedNotification(Long notificationId);

    /**
     * Save task notification configuration
     */
    void saveTaskNotificationConfig(Long taskId, NotificationConfig config);

    /**
     * Save workflow notification configuration
     */
    void saveWorkflowNotificationConfig(Long workflowId, NotificationConfig config);

    /**
     * Get task notification configurations
     */
    List<NotificationConfig> getTaskNotificationConfigs(Long taskId);

    /**
     * Get workflow notification configurations
     */
    List<NotificationConfig> getWorkflowNotificationConfigs(Long workflowId);

    /**
     * Delete task notification configuration
     */
    void deleteTaskNotificationConfig(Long taskId, Long configId);

    /**
     * Delete workflow notification configuration
     */
    void deleteWorkflowNotificationConfig(Long workflowId, Long configId);

    /**
     * Update task notification configuration
     */
    void updateTaskNotificationConfig(Long taskId, NotificationConfig config);

    /**
     * Update workflow notification configuration
     */
    void updateWorkflowNotificationConfig(Long workflowId, NotificationConfig config);

    /**
     * Get notification templates by type
     */
    Map<String, String> getNotificationTemplates(String type);

    /**
     * Save notification template
     */
    void saveNotificationTemplate(String type, String name, String template);

    /**
     * Delete notification template
     */
    void deleteNotificationTemplate(String type, String name);

    /**
     * Get notification recipients by type
     */
    List<String> getNotificationRecipients(String type);

    /**
     * Save notification recipient
     */
    void saveNotificationRecipient(String type, String recipient);

    /**
     * Delete notification recipient
     */
    void deleteNotificationRecipient(String type, String recipient);

    /**
     * Get notification settings
     */
    Map<String, Object> getNotificationSettings(Long tenantId);

    /**
     * Save notification settings
     */
    void saveNotificationSettings(Long tenantId, Map<String, Object> settings);

    /**
     * Get notification rate limits
     */
    Map<String, Integer> getNotificationRateLimits(Long tenantId);

    /**
     * Save notification rate limits
     */
    void saveNotificationRateLimits(Long tenantId, Map<String, Integer> rateLimits);

    /**
     * Clean up old notification history
     */
    void cleanupNotificationHistory(int daysToKeep);

    /**
     * Get notification summary for dashboard
     */
    Map<String, Object> getNotificationSummary(Long tenantId);
}
