package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;
import java.util.HashSet;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    public enum UserRole {
        ADMIN,          // Full system access
        MANAGER,        // Can manage workflows and tasks
        OPERATOR,       // Can execute and monitor tasks
        VIEWER          // Read-only access
    }

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED
    }

    private String username;
    private String password;        // Hashed password
    private String salt;            // Salt for password hashing
    private String email;
    private String phone;
    private String realName;
    private UserStatus status;
    
    // User roles (stored as comma-separated string in DB)
    private String roles;
    
    // Notification preferences
    private Boolean emailNotification;
    private Boolean wechatNotification;
    private String wechatId;
    
    // Login tracking
    private String lastLoginIp;
    private java.time.LocalDateTime lastLoginTime;
    private Integer loginFailCount;
    private java.time.LocalDateTime lockTime;
    
    // Additional settings
    private String preferences;     // JSON string of user preferences
    private String timezone;
    private String language;        // e.g., "en_US", "zh_CN"

    public User() {
        this.status = UserStatus.ACTIVE;
        this.emailNotification = true;
        this.wechatNotification = false;
        this.loginFailCount = 0;
        this.timezone = "UTC";
        this.language = "en_US";
    }

    /**
     * Get user roles as a Set
     */
    public Set<UserRole> getRoleSet() {
        Set<UserRole> roleSet = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles.split(",")) {
                try {
                    roleSet.add(UserRole.valueOf(role.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid role
                }
            }
        }
        return roleSet;
    }

    /**
     * Set user roles from a Set
     */
    public void setRoleSet(Set<UserRole> roleSet) {
        if (roleSet == null || roleSet.isEmpty()) {
            this.roles = "";
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (UserRole role : roleSet) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(role.name());
        }
        this.roles = sb.toString();
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(UserRole role) {
        return getRoleSet().contains(role);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * Check if account is not locked and active
     */
    public boolean isAccountNonLocked() {
        if (this.status != UserStatus.LOCKED) {
            return true;
        }
        // Check if lock time has expired (default lock time: 30 minutes)
        if (this.lockTime != null && 
            this.lockTime.plusMinutes(30).isBefore(java.time.LocalDateTime.now())) {
            this.status = UserStatus.ACTIVE;
            this.loginFailCount = 0;
            this.lockTime = null;
            return true;
        }
        return false;
    }

    /**
     * Record failed login attempt
     */
    public void recordLoginFailure() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {  // Lock after 5 failed attempts
            this.status = UserStatus.LOCKED;
            this.lockTime = java.time.LocalDateTime.now();
        }
    }

    /**
     * Record successful login
     */
    public void recordLoginSuccess(String ip) {
        this.lastLoginIp = ip;
        this.lastLoginTime = java.time.LocalDateTime.now();
        this.loginFailCount = 0;
        this.lockTime = null;
    }

    /**
     * Check if user can receive notifications
     */
    public boolean canReceiveNotifications() {
        return this.status == UserStatus.ACTIVE && 
              (this.emailNotification || this.wechatNotification);
    }
}
