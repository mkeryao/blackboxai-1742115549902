package com.jobflow.dao.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobflow.dao.NotificationDao;
import com.jobflow.domain.NotificationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class NotificationJdbcDao implements NotificationDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveFailedNotification(String type, Long sourceId, NotificationConfig.NotificationType eventType,
                                     NotificationConfig config, String errorMessage) {
        String sql = """
            INSERT INTO fj_notification_history (
                type, source_id, event_type, channel, recipient, content,
                status, error_message, tenant_id, created_by, created_time
            ) VALUES (
                :type, :sourceId, :eventType, :channel, :recipient, :content,
                'FAILED', :errorMessage, :tenantId, :createdBy, :createdTime
            )
        """;

        for (NotificationConfig.NotificationChannel channel : config.getChannels()) {
            for (String recipient : config.getRecipients()) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("type", type)
                    .addValue("sourceId", sourceId)
                    .addValue("eventType", eventType.name())
                    .addValue("channel", channel.name())
                    .addValue("recipient", recipient)
                    .addValue("content", config.getTemplate())
                    .addValue("errorMessage", errorMessage)
                    .addValue("tenantId", getCurrentTenantId())
                    .addValue("createdBy", getCurrentUserId())
                    .addValue("createdTime", LocalDateTime.now());

                namedParameterJdbcTemplate.update(sql, params);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getNotificationHistory(String type, Long sourceId) {
        String sql = """
            SELECT * FROM fj_notification_history
            WHERE type = ? AND source_id = ? AND tenant_id = ?
            ORDER BY created_time DESC
        """;
        return jdbcTemplate.queryForList(sql, type, sourceId, getCurrentTenantId());
    }

    @Override
    public Map<String, Object> getNotificationStatistics(Long tenantId) {
        String sql = """
            SELECT 
                COUNT(*) as total_count,
                SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END) as success_count,
                SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
                COUNT(DISTINCT source_id) as source_count,
                COUNT(DISTINCT recipient) as recipient_count
            FROM fj_notification_history
            WHERE tenant_id = ? AND created_time >= ?
        """;
        return jdbcTemplate.queryForMap(sql, tenantId, LocalDateTime.now().minusDays(30));
    }

    @Override
    public List<Map<String, Object>> getFailedNotifications() {
        String sql = """
            SELECT * FROM fj_notification_history
            WHERE status = 'FAILED' AND tenant_id = ?
            ORDER BY created_time DESC
        """;
        return jdbcTemplate.queryForList(sql, getCurrentTenantId());
    }

    @Override
    @Transactional
    public boolean retryFailedNotification(Long notificationId) {
        String sql = """
            UPDATE fj_notification_history
            SET status = 'RETRY', updated_by = ?, updated_time = ?
            WHERE id = ? AND tenant_id = ? AND status = 'FAILED'
        """;
        int updated = jdbcTemplate.update(sql, getCurrentUserId(), LocalDateTime.now(),
                                        notificationId, getCurrentTenantId());
        return updated > 0;
    }

    @Override
    @Transactional
    public void saveTaskNotificationConfig(Long taskId, NotificationConfig config) {
        String sql = """
            INSERT INTO fj_task_notification (
                task_id, notify_on, channel, recipient, template,
                enabled, tenant_id, created_by, created_time
            ) VALUES (
                :taskId, :notifyOn, :channel, :recipient, :template,
                :enabled, :tenantId, :createdBy, :createdTime
            )
        """;

        for (NotificationConfig.NotificationType notifyOn : config.getNotifyOn()) {
            for (NotificationConfig.NotificationChannel channel : config.getChannels()) {
                for (String recipient : config.getRecipients()) {
                    MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("taskId", taskId)
                        .addValue("notifyOn", notifyOn.name())
                        .addValue("channel", channel.name())
                        .addValue("recipient", recipient)
                        .addValue("template", config.getTemplate())
                        .addValue("enabled", config.isEnabled())
                        .addValue("tenantId", getCurrentTenantId())
                        .addValue("createdBy", getCurrentUserId())
                        .addValue("createdTime", LocalDateTime.now());

                    namedParameterJdbcTemplate.update(sql, params);
                }
            }
        }
    }

    @Override
    @Transactional
    public void saveWorkflowNotificationConfig(Long workflowId, NotificationConfig config) {
        String sql = """
            INSERT INTO fj_workflow_notification (
                workflow_id, notify_on, channel, recipient, template,
                enabled, tenant_id, created_by, created_time
            ) VALUES (
                :workflowId, :notifyOn, :channel, :recipient, :template,
                :enabled, :tenantId, :createdBy, :createdTime
            )
        """;

        for (NotificationConfig.NotificationType notifyOn : config.getNotifyOn()) {
            for (NotificationConfig.NotificationChannel channel : config.getChannels()) {
                for (String recipient : config.getRecipients()) {
                    MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("workflowId", workflowId)
                        .addValue("notifyOn", notifyOn.name())
                        .addValue("channel", channel.name())
                        .addValue("recipient", recipient)
                        .addValue("template", config.getTemplate())
                        .addValue("enabled", config.isEnabled())
                        .addValue("tenantId", getCurrentTenantId())
                        .addValue("createdBy", getCurrentUserId())
                        .addValue("createdTime", LocalDateTime.now());

                    namedParameterJdbcTemplate.update(sql, params);
                }
            }
        }
    }

    @Override
    public List<NotificationConfig> getTaskNotificationConfigs(Long taskId) {
        String sql = """
            SELECT * FROM fj_task_notification
            WHERE task_id = ? AND tenant_id = ?
            ORDER BY created_time DESC
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, taskId, getCurrentTenantId());
        return rows.stream()
            .map(this::mapToNotificationConfig)
            .toList();
    }

    @Override
    public List<NotificationConfig> getWorkflowNotificationConfigs(Long workflowId) {
        String sql = """
            SELECT * FROM fj_workflow_notification
            WHERE workflow_id = ? AND tenant_id = ?
            ORDER BY created_time DESC
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, workflowId, getCurrentTenantId());
        return rows.stream()
            .map(this::mapToNotificationConfig)
            .toList();
    }

    private NotificationConfig mapToNotificationConfig(Map<String, Object> row) {
        NotificationConfig config = new NotificationConfig();
        config.setNotifyOn(List.of(NotificationConfig.NotificationType.valueOf((String) row.get("notify_on"))));
        config.setChannels(List.of(NotificationConfig.NotificationChannel.valueOf((String) row.get("channel"))));
        config.setRecipients(List.of((String) row.get("recipient")));
        config.setTemplate((String) row.get("template"));
        config.setEnabled((Boolean) row.get("enabled"));
        return config;
    }

    private Long getCurrentTenantId() {
        // Implement based on your tenant management system
        return 1L;
    }

    private String getCurrentUserId() {
        // Implement based on your user management system
        return "system";
    }

    // ... implement remaining methods ...
}
