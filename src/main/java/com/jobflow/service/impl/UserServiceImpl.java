package com.jobflow.service.impl;

import com.jobflow.dao.UserDao;
import com.jobflow.dao.OperationLogDao;
import com.jobflow.domain.User;
import com.jobflow.domain.OperationLog;
import com.jobflow.service.AbstractBaseService;
import com.jobflow.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl extends AbstractBaseService<User> implements UserService {

    private final UserDao userDao;
    private final SecureRandom secureRandom;
    private static final int SALT_LENGTH = 16;

    @Autowired
    public UserServiceImpl(UserDao userDao, OperationLogDao operationLogDao) {
        super(userDao, operationLogDao);
        this.userDao = userDao;
        this.secureRandom = new SecureRandom();
    }

    @Override
    protected OperationLog.OperationModule getOperationModule() {
        return OperationLog.OperationModule.USER;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    @Override
    @Transactional
    public User authenticate(String username, String password, Long tenantId, String ip) {
        User user = userDao.findByUsername(username, tenantId);
        
        if (user == null) {
            log.warn("Authentication failed: user not found - {}", username);
            return null;
        }

        if (!user.isAccountNonLocked()) {
            log.warn("Authentication failed: account locked - {}", username);
            return null;
        }

        String hashedPassword = hashPassword(password, user.getSalt());
        if (!hashedPassword.equals(user.getPassword())) {
            user.recordLoginFailure();
            userDao.update(user, "system");
            log.warn("Authentication failed: invalid password - {}", username);
            return null;
        }

        user.recordLoginSuccess(ip);
        userDao.updateLoginSuccess(user.getId(), ip, "system");
        
        logOperation("system", 
                    OperationLog.OperationType.LOGIN,
                    user.getId().toString(),
                    "User login successful",
                    String.format("IP: %s", ip));
        
        return user;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword, String operator) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }

        String oldHashedPassword = hashPassword(oldPassword, user.getSalt());
        if (!oldHashedPassword.equals(user.getPassword())) {
            return false;
        }

        String newSalt = generateSalt();
        String newHashedPassword = hashPassword(newPassword, newSalt);
        
        user.setPassword(newHashedPassword);
        user.setSalt(newSalt);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "Password changed",
                    null);
        
        return true;
    }

    @Override
    @Transactional
    public String resetPassword(Long userId, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        String newPassword = generateRandomPassword();
        String newSalt = generateSalt();
        String hashedPassword = hashPassword(newPassword, newSalt);
        
        user.setPassword(hashedPassword);
        user.setSalt(newSalt);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "Password reset",
                    null);
        
        return newPassword;
    }

    @Override
    @Transactional
    public void lockUser(Long userId, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setStatus(User.UserStatus.LOCKED);
        user.setLockTime(LocalDateTime.now());
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "User account locked",
                    null);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setStatus(User.UserStatus.ACTIVE);
        user.setLockTime(null);
        user.setLoginFailCount(0);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "User account unlocked",
                    null);
    }

    @Override
    @Transactional
    public void updateRoles(Long userId, Set<User.UserRole> roles, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setRoleSet(roles);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "User roles updated",
                    String.format("Roles: %s", roles));
    }

    @Override
    public User findByUsername(String username, Long tenantId) {
        return userDao.findByUsername(username, tenantId);
    }

    @Override
    public List<User> findByRole(User.UserRole role, Long tenantId) {
        return userDao.findByRole(role.name(), tenantId);
    }

    @Override
    @Transactional
    public void updateNotificationPreferences(Long userId, Boolean emailNotification,
                                            Boolean wechatNotification, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setEmailNotification(emailNotification);
        user.setWechatNotification(wechatNotification);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "Notification preferences updated",
                    String.format("Email: %b, WeChat: %b", emailNotification, wechatNotification));
    }

    @Override
    @Transactional
    public void updateLanguage(Long userId, String language, String operator) {
        User user = findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setLanguage(language);
        userDao.update(user, operator);

        logOperation(operator,
                    OperationLog.OperationType.UPDATE,
                    userId.toString(),
                    "Language preference updated",
                    String.format("Language: %s", language));
    }

    @Override
    public UserStatistics getUserStatistics(Long tenantId) {
        List<User> users = findByTenantId(tenantId);
        UserStatistics stats = new UserStatistics();
        
        stats.setTotalUsers(users.size());
        stats.setActiveUsers(users.stream()
            .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
            .count());
        stats.setLockedUsers(users.stream()
            .filter(u -> u.getStatus() == User.UserStatus.LOCKED)
            .count());
        stats.setAdminUsers(users.stream()
            .filter(u -> u.hasRole(User.UserRole.ADMIN))
            .count());
        stats.setManagerUsers(users.stream()
            .filter(u -> u.hasRole(User.UserRole.MANAGER))
            .count());
        stats.setOperatorUsers(users.stream()
            .filter(u -> u.hasRole(User.UserRole.OPERATOR))
            .count());
        stats.setViewerUsers(users.stream()
            .filter(u -> u.hasRole(User.UserRole.VIEWER))
            .count());
        
        // Consider users who logged in within the last hour as online
        stats.setOnlineUsers(users.stream()
            .filter(u -> u.getLastLoginTime() != null && 
                        u.getLastLoginTime().isAfter(LocalDateTime.now().minusHours(1)))
            .count());
        
        // Calculate average logins per day (based on the last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long totalLogins = users.stream()
            .filter(u -> u.getLastLoginTime() != null && 
                        u.getLastLoginTime().isAfter(thirtyDaysAgo))
            .count();
        stats.setAverageLoginPerDay((double) totalLogins / 30);
        
        return stats;
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }

        // Admin has all permissions
        if (user.hasRole(User.UserRole.ADMIN)) {
            return true;
        }

        // TODO: Implement detailed permission checking based on roles
        return false;
    }

    @Override
    public Map<String, List<Long>> getAccessibleResources(Long userId) {
        // TODO: Implement resource access control based on user roles
        return new HashMap<>();
    }

    @Override
    public List<UserActivity> getRecentActivities(Long userId, int limit) {
        // TODO: Implement recent activities tracking
        return new ArrayList<>();
    }

    private String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return password.toString();
    }
}
