package com.jobflow.dao;

import com.jobflow.domain.Notification;
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
public class NotificationDao extends BaseDao<Notification> {

    private static final String TABLE_NAME = "fj_notification";

    private static final RowMapper<Notification> ROW_MAPPER = (rs, rowNum) -> {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setTenantId(rs.getLong("tenant_id"));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setType(Notification.NotificationType.valueOf(rs.getString("type")));
        notification.setStatus(Notification.NotificationStatus.valueOf(rs.getString("status")));
        notification.setLevel(Notification.NotificationLevel.valueOf(rs.getString("level")));
        notification.setSource(Notification.NotificationSource.valueOf(rs.getString("source")));
        notification.setSourceId(rs.getLong("source_id"));
        notification.setSourceName(rs.getString("source_name"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setRecipient(rs.getString("recipient"));
        notification.setMaxRetries(rs.getInt("max_retries"));
        notification.setCurrentRetries(rs.getInt("current_retries"));
        notification.setRetryInterval(rs.getLong("retry_interval"));
        
        Timestamp scheduledTime = rs.getTimestamp("scheduled_time");
        if (scheduledTime != null) {
            notification.setScheduledTime(scheduledTime.toLocalDateTime());
        }
        
        Timestamp sentTime = rs.getTimestamp("sent_time");
        if (sentTime != null) {
            notification.setSentTime(sentTime.toLocalDateTime());
        }
        
        notification.setErrorMessage(rs.getString("error_message"));
        notification.setResponseData(rs.getString("response_data"));
        notification.setParameters(rs.getString("parameters"));
        notification.setCreatedBy(rs.getString("created_by"));
        
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            notification.setCreatedTime(createdTime.toLocalDateTime());
        }
        
        notification.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            notification.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        
        notification.setDeleted(rs.getBoolean("deleted"));
        notification.setVersion(rs.getInt("version"));
        
        return notification;
    };

    public NotificationDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, TABLE_NAME, ROW_MAPPER);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + TABLE_NAME + " (tenant_id, title, content, type, status, level, " +
               "source, source_id, source_name, user_id, recipient, max_retries, current_retries, " +
               "retry_interval, scheduled_time, sent_time, error_message, response_data, parameters, " +
               "created_by, created_time, updated_by, updated_time, deleted, version) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Notification notification) throws SQLException {
        int i = 1;
        ps.setLong(i++, notification.getTenantId());
        ps.setString(i++, notification.getTitle());
        ps.setString(i++, notification.getContent());
        ps.setString(i++, notification.getType().name());
        ps.setString(i++, notification.getStatus().name());
        ps.setString(i++, notification.getLevel().name());
        ps.setString(i++, notification.getSource().name());
        ps.setLong(i++, notification.getSourceId());
        ps.setString(i++, notification.getSourceName());
        ps.setLong(i++, notification.getUserId());
        ps.setString(i++, notification.getRecipient());
        ps.setInt(i++, notification.getMaxRetries());
        ps.setInt(i++, notification.getCurrentRetries());
        ps.setLong(i++, notification.getRetryInterval());
        ps.setTimestamp(i++, Timestamp.valueOf(notification.getScheduledTime()));
        ps.setTimestamp(i++, notification.getSentTime() != null ? Timestamp.valueOf(notification.getSentTime()) : null);
        ps.setString(i++, notification.getErrorMessage());
        ps.setString(i++, notification.getResponseData());
        ps.setString(i++, notification.getParameters());
        ps.setString(i++, notification.getCreatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(notification.getCreatedTime()));
        ps.setString(i++, notification.getUpdatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(notification.getUpdatedTime()));
        ps.setBoolean(i++, notification.getDeleted());
        ps.setInt(i, notification.getVersion());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE " + TABLE_NAME + " SET status = ?, current_retries = ?, sent_time = ?, " +
               "error_message = ?, response_data = ?, updated_by = ?, updated_time = ?, " +
               "version = version + 1 " +
               "WHERE id = ? AND version = ? AND deleted = false";
    }

    @Override
    protected Object[] getUpdateParameters(Notification notification) {
        return new Object[]{
            notification.getStatus().name(),
            notification.getCurrentRetries(),
            notification.getSentTime() != null ? Timestamp.valueOf(notification.getSentTime()) : null,
            notification.getErrorMessage(),
            notification.getResponseData(),
            notification.getUpdatedBy(),
            Timestamp.valueOf(notification.getUpdatedTime()),
            notification.getId(),
            notification.getVersion()
        };
    }

    /**
     * Find pending notifications ready to be sent
     */
    public List<Notification> findPendingNotifications() {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE status = 'PENDING' AND scheduled_time <= ? " +
                    " AND deleted = false ORDER BY scheduled_time ASC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, 
                                    Timestamp.valueOf(java.time.LocalDateTime.now()));
        } catch (DataAccessException e) {
            log.error("Failed to find pending notifications: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find failed notifications eligible for retry
     */
    public List<Notification> findRetryableNotifications() {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE status = 'FAILED' AND current_retries < max_retries " +
                    " AND deleted = false ORDER BY scheduled_time ASC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            log.error("Failed to find retryable notifications: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find notifications by source
     */
    public List<Notification> findBySource(Notification.NotificationSource source, 
                                         Long sourceId, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE source = ? AND source_id = ? AND tenant_id = ? " +
                    " AND deleted = false ORDER BY scheduled_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, source.name(), sourceId, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find notifications by source: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find notifications by user
     */
    public List<Notification> findByUser(Long userId, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE user_id = ? AND tenant_id = ? AND deleted = false " +
                    " ORDER BY scheduled_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, userId, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find notifications by user: {}", e.getMessage());
            throw e;
        }
    }
}
