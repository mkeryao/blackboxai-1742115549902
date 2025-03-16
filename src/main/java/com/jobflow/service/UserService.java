package com.jobflow.service;

import com.jobflow.domain.User;
import java.util.List;
import java.util.Set;

/**
 * Service interface for managing users
 */
public interface UserService extends BaseService<User> {
    
    /**
     * Authenticate user
     * @param username Username
     * @param password Raw password
     * @param tenantId Tenant ID
     * @param ip Client IP address
     * @return User if authentication successful, null otherwise
     */
    User authenticate(String username, String password, Long tenantId, String ip);

    /**
     * Change user password
     * @param userId User ID
     * @param oldPassword Old password
     * @param newPassword New password
     * @param operator User performing the operation
     * @return true if password changed successfully
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword, String operator);

    /**
     * Reset user password
     * @param userId User ID
     * @param operator User performing the operation
     * @return New password
     */
    String resetPassword(Long userId, String operator);

    /**
     * Lock user account
     * @param userId User ID
     * @param operator User performing the operation
     */
    void lockUser(Long userId, String operator);

    /**
     * Unlock user account
     * @param userId User ID
     * @param operator User performing the operation
     */
    void unlockUser(Long userId, String operator);

    /**
     * Update user roles
     * @param userId User ID
     * @param roles Set of roles
     * @param operator User performing the operation
     */
    void updateRoles(Long userId, Set<User.UserRole> roles, String operator);

    /**
     * Find user by username
     * @param username Username
     * @param tenantId Tenant ID
     * @return User if found, null otherwise
     */
    User findByUsername(String username, Long tenantId);

    /**
     * Find users by role
     * @param role Role to search for
     * @param tenantId Tenant ID
     * @return List of users with the specified role
     */
    List<User> findByRole(User.UserRole role, Long tenantId);

    /**
     * Update notification preferences
     * @param userId User ID
     * @param emailNotification Enable/disable email notifications
     * @param wechatNotification Enable/disable WeChat notifications
     * @param operator User performing the operation
     */
    void updateNotificationPreferences(Long userId, Boolean emailNotification, 
                                     Boolean wechatNotification, String operator);

    /**
     * Update user language preference
     * @param userId User ID
     * @param language Language code (e.g., "en_US", "zh_CN")
     * @param operator User performing the operation
     */
    void updateLanguage(Long userId, String language, String operator);

    /**
     * Get user statistics
     * @param tenantId Tenant ID
     * @return User statistics
     */
    UserStatistics getUserStatistics(Long tenantId);

    /**
     * Inner class for user statistics
     */
    class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long lockedUsers;
        private long adminUsers;
        private long managerUsers;
        private long operatorUsers;
        private long viewerUsers;
        private long onlineUsers;
        private double averageLoginPerDay;

        // Getters and setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        
        public long getLockedUsers() { return lockedUsers; }
        public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }
        
        public long getAdminUsers() { return adminUsers; }
        public void setAdminUsers(long adminUsers) { this.adminUsers = adminUsers; }
        
        public long getManagerUsers() { return managerUsers; }
        public void setManagerUsers(long managerUsers) { this.managerUsers = managerUsers; }
        
        public long getOperatorUsers() { return operatorUsers; }
        public void setOperatorUsers(long operatorUsers) { this.operatorUsers = operatorUsers; }
        
        public long getViewerUsers() { return viewerUsers; }
        public void setViewerUsers(long viewerUsers) { this.viewerUsers = viewerUsers; }
        
        public long getOnlineUsers() { return onlineUsers; }
        public void setOnlineUsers(long onlineUsers) { this.onlineUsers = onlineUsers; }
        
        public double getAverageLoginPerDay() { return averageLoginPerDay; }
        public void setAverageLoginPerDay(double averageLoginPerDay) { 
            this.averageLoginPerDay = averageLoginPerDay; 
        }
    }

    /**
     * Check if user has specific permission
     * @param userId User ID
     * @param permission Permission to check
     * @return true if user has permission
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * Get user's accessible resources
     * @param userId User ID
     * @return Map of resource types to lists of resource IDs
     */
    Map<String, List<Long>> getAccessibleResources(Long userId);

    /**
     * Get user's recent activities
     * @param userId User ID
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    List<UserActivity> getRecentActivities(Long userId, int limit);

    /**
     * Inner class for user activity
     */
    class UserActivity {
        private LocalDateTime timestamp;
        private String activityType;
        private String resourceType;
        private String resourceId;
        private String description;
        private String ipAddress;

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }
        
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }
}
