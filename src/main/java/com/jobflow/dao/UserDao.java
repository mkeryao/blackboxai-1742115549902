package com.jobflow.dao;

import com.jobflow.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Repository
public class UserDao extends BaseDao<User> {

    private static final String TABLE_NAME = "fj_user";

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setTenantId(rs.getLong("tenant_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setSalt(rs.getString("salt"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setRealName(rs.getString("real_name"));
        user.setStatus(User.UserStatus.valueOf(rs.getString("status")));
        user.setRoles(rs.getString("roles"));
        user.setEmailNotification(rs.getBoolean("email_notification"));
        user.setWechatNotification(rs.getBoolean("wechat_notification"));
        user.setWechatId(rs.getString("wechat_id"));
        user.setLastLoginIp(rs.getString("last_login_ip"));
        
        Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
        if (lastLoginTime != null) {
            user.setLastLoginTime(lastLoginTime.toLocalDateTime());
        }
        
        user.setLoginFailCount(rs.getInt("login_fail_count"));
        
        Timestamp lockTime = rs.getTimestamp("lock_time");
        if (lockTime != null) {
            user.setLockTime(lockTime.toLocalDateTime());
        }
        
        user.setPreferences(rs.getString("preferences"));
        user.setTimezone(rs.getString("timezone"));
        user.setLanguage(rs.getString("language"));
        user.setCreatedBy(rs.getString("created_by"));
        
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            user.setCreatedTime(createdTime.toLocalDateTime());
        }
        
        user.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            user.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        
        user.setDeleted(rs.getBoolean("deleted"));
        user.setVersion(rs.getInt("version"));
        
        return user;
    };

    public UserDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, TABLE_NAME, ROW_MAPPER);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + TABLE_NAME + " (tenant_id, username, password, salt, email, phone, " +
               "real_name, status, roles, email_notification, wechat_notification, wechat_id, " +
               "last_login_ip, last_login_time, login_fail_count, lock_time, preferences, timezone, " +
               "language, created_by, created_time, updated_by, updated_time, deleted, version) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, User user) throws SQLException {
        int i = 1;
        ps.setLong(i++, user.getTenantId());
        ps.setString(i++, user.getUsername());
        ps.setString(i++, user.getPassword());
        ps.setString(i++, user.getSalt());
        ps.setString(i++, user.getEmail());
        ps.setString(i++, user.getPhone());
        ps.setString(i++, user.getRealName());
        ps.setString(i++, user.getStatus().name());
        ps.setString(i++, user.getRoles());
        ps.setBoolean(i++, user.getEmailNotification());
        ps.setBoolean(i++, user.getWechatNotification());
        ps.setString(i++, user.getWechatId());
        ps.setString(i++, user.getLastLoginIp());
        ps.setTimestamp(i++, user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);
        ps.setInt(i++, user.getLoginFailCount());
        ps.setTimestamp(i++, user.getLockTime() != null ? Timestamp.valueOf(user.getLockTime()) : null);
        ps.setString(i++, user.getPreferences());
        ps.setString(i++, user.getTimezone());
        ps.setString(i++, user.getLanguage());
        ps.setString(i++, user.getCreatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(user.getCreatedTime()));
        ps.setString(i++, user.getUpdatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(user.getUpdatedTime()));
        ps.setBoolean(i++, user.getDeleted());
        ps.setInt(i, user.getVersion());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE " + TABLE_NAME + " SET password = ?, salt = ?, email = ?, phone = ?, " +
               "real_name = ?, status = ?, roles = ?, email_notification = ?, " +
               "wechat_notification = ?, wechat_id = ?, preferences = ?, timezone = ?, " +
               "language = ?, updated_by = ?, updated_time = ?, version = version + 1 " +
               "WHERE id = ? AND version = ? AND deleted = false";
    }

    @Override
    protected Object[] getUpdateParameters(User user) {
        return new Object[]{
            user.getPassword(),
            user.getSalt(),
            user.getEmail(),
            user.getPhone(),
            user.getRealName(),
            user.getStatus().name(),
            user.getRoles(),
            user.getEmailNotification(),
            user.getWechatNotification(),
            user.getWechatId(),
            user.getPreferences(),
            user.getTimezone(),
            user.getLanguage(),
            user.getUpdatedBy(),
            Timestamp.valueOf(user.getUpdatedTime()),
            user.getId(),
            user.getVersion()
        };
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE username = ? AND tenant_id = ? AND deleted = false";
        try {
            return jdbcTemplate.queryForObject(sql, ROW_MAPPER, username, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find user by username: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Update login success
     */
    public boolean updateLoginSuccess(Long id, String ip, String operator) {
        String sql = "UPDATE " + TABLE_NAME + 
                    " SET last_login_ip = ?, last_login_time = ?, login_fail_count = 0, " +
                    "lock_time = NULL, updated_by = ?, updated_time = ?, version = version + 1 " +
                    "WHERE id = ? AND deleted = false";
        try {
            int rows = jdbcTemplate.update(sql, ip, Timestamp.valueOf(java.time.LocalDateTime.now()),
                                         operator, Timestamp.valueOf(java.time.LocalDateTime.now()), id);
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to update login success: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update login failure
     */
    public boolean updateLoginFailure(Long id, String operator) {
        String sql = "UPDATE " + TABLE_NAME + 
                    " SET login_fail_count = login_fail_count + 1, " +
                    "status = CASE WHEN login_fail_count >= 4 THEN 'LOCKED' ELSE status END, " +
                    "lock_time = CASE WHEN login_fail_count >= 4 THEN ? ELSE lock_time END, " +
                    "updated_by = ?, updated_time = ?, version = version + 1 " +
                    "WHERE id = ? AND deleted = false";
        try {
            int rows = jdbcTemplate.update(sql, Timestamp.valueOf(java.time.LocalDateTime.now()),
                                         operator, Timestamp.valueOf(java.time.LocalDateTime.now()), id);
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to update login failure: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find users by role
     */
    public List<User> findByRole(String role, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE roles LIKE ? AND tenant_id = ? AND deleted = false";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, "%" + role + "%", tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find users by role: {}", e.getMessage());
            throw e;
        }
    }
}
