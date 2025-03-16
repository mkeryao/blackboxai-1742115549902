package com.jobflow.service;

import com.jobflow.domain.Notification;
import com.jobflow.domain.Task;
import com.jobflow.domain.User;
import com.jobflow.domain.Workflow;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing notifications
 */
public interface NotificationService extends BaseService<Notification> {
    
    /**
     * Send notification
     * @param notification Notification to send
     * @param operator User performing the operation
     * @return true if sent successfully
     */
    boolean sendNotification(Notification notification, String operator);

    /**
     * Send task notification
     * @param task Task
     * @param user User to notify
     * @param type Notification type
     * @param level Notification level
     * @param content Notification content
     * @param operator User performing the operation
     * @return true if sent successfully
     */
    boolean sendTaskNotification(Task task, User user, 
                               Notification.NotificationType type,
                               Notification.NotificationLevel level,
                               String content, String operator);

    /**
     * Send workflow notification
     * @param workflow Workflow
     * @param user User to notify
     * @param type Notification type
     * @param level Notification level
     * @param content Notification content
     * @param operator User performing the operation
     * @return true if sent successfully
     */
    boolean sendWorkflowNotification(Workflow workflow, User user,
                                   Notification.NotificationType type,
                                   Notification.NotificationLevel level,
                                   String content, String operator);

    /**
     * Send system notification
     * @param users List of users to notify
     * @param type Notification type
     * @param level Notification level
     * @param title Notification title
     * @param content Notification content
     * @param operator User performing the operation
     * @return Number of notifications sent successfully
     */
    int sendSystemNotification(List<User> users,
                             Notification.NotificationType type,
                             Notification.NotificationLevel level,
                             String title, String content, String operator);

    /**
     * Retry failed notification
     * @param notificationId Notification ID
     * @param operator User performing the operation
     * @return true if retry successful
     */
    boolean retryNotification(Long notificationId, String operator);

    /**
     * Cancel pending notification
     * @param notificationId Notification ID
     * @param operator User performing the operation
     */
    void cancelNotification(Long notificationId, String operator);

    /**
     * Find pending notifications
     * @return List of pending notifications
     */
    List<Notification> findPendingNotifications();

    /**
     * Find failed notifications eligible for retry
     * @return List of retryable notifications
     */
    List<Notification> findRetryableNotifications();

    /**
     * Find notifications by source
     * @param source Notification source
     * @param sourceId Source ID
     * @param tenantId Tenant ID
     * @return List of notifications
     */
    List<Notification> findBySource(Notification.NotificationSource source, 
                                  Long sourceId, Long tenantId);

    /**
     * Find notifications by user
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return List of notifications
     */
    List<Notification> findByUser(Long userId, Long tenantId);

    /**
     * Get notification statistics
     * @param tenantId Tenant ID
     * @return Notification statistics
     */
    NotificationStatistics getNotificationStatistics(Long tenantId);

    /**
     * Test notification channel
     * @param type Notification type
     * @param recipient Recipient (email/wechat/webhook)
     * @param operator User performing the operation
     * @return true if test successful
     */
    boolean testNotificationChannel(Notification.NotificationType type, 
                                  String recipient, String operator);

    /**
     * Inner class for notification statistics
     */
    class NotificationStatistics {
        private long totalNotifications;
        private long pendingNotifications;
        private long sentNotifications;
        private long failedNotifications;
        private Map<Notification.NotificationType, Long> notificationsByType;
        private Map<Notification.NotificationLevel, Long> notificationsByLevel;
        private Map<Notification.NotificationSource, Long> notificationsBySource;
        private double averageDeliveryTime;
        private double successRate;

        // Getters and setters
        public long getTotalNotifications() { return totalNotifications; }
        public void setTotalNotifications(long totalNotifications) { 
            this.totalNotifications = totalNotifications; 
        }
        
        public long getPendingNotifications() { return pendingNotifications; }
        public void setPendingNotifications(long pendingNotifications) { 
            this.pendingNotifications = pendingNotifications; 
        }
        
        public long getSentNotifications() { return sentNotifications; }
        public void setSentNotifications(long sentNotifications) { 
            this.sentNotifications = sentNotifications; 
        }
        
        public long getFailedNotifications() { return failedNotifications; }
        public void setFailedNotifications(long failedNotifications) { 
            this.failedNotifications = failedNotifications; 
        }
        
        public Map<Notification.NotificationType, Long> getNotificationsByType() { 
            return notificationsByType; 
        }
        public void setNotificationsByType(Map<Notification.NotificationType, Long> notificationsByType) { 
            this.notificationsByType = notificationsByType; 
        }
        
        public Map<Notification.NotificationLevel, Long> getNotificationsByLevel() { 
            return notificationsByLevel; 
        }
        public void setNotificationsByLevel(Map<Notification.NotificationLevel, Long> notificationsByLevel) { 
            this.notificationsByLevel = notificationsByLevel; 
        }
        
        public Map<Notification.NotificationSource, Long> getNotificationsBySource() { 
            return notificationsBySource; 
        }
        public void setNotificationsBySource(Map<Notification.NotificationSource, Long> notificationsBySource) { 
            this.notificationsBySource = notificationsBySource; 
        }
        
        public double getAverageDeliveryTime() { return averageDeliveryTime; }
        public void setAverageDeliveryTime(double averageDeliveryTime) { 
            this.averageDeliveryTime = averageDeliveryTime; 
        }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}
