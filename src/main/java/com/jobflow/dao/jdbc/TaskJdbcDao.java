package com.jobflow.dao.jdbc;

import com.jobflow.dao.TaskDao;
import com.jobflow.domain.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskJdbcDao implements TaskDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<Task> rowMapper = new TaskRowMapper();

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        } else {
            return update(task);
        }
    }

    private Task insert(Task task) {
        String sql = """
            INSERT INTO fj_task (
                name, description, command, cron, timeout, retries, retry_delay,
                status, priority, start_time, end_time, workflow_id, sequence,
                parameters, notification, tenant_id, created_by, created_time,
                updated_by, updated_time
            ) VALUES (
                :name, :description, :command, :cron, :timeout, :retries, :retryDelay,
                :status, :priority, :startTime, :endTime, :workflowId, :sequence,
                :parameters, :notification, :tenantId, :createdBy, :createdTime,
                :updatedBy, :updatedTime
            )
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParameterSource(task);

        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        task.setId(keyHolder.getKey().longValue());
        return task;
    }

    private Task update(Task task) {
        String sql = """
            UPDATE fj_task SET
                name = :name, description = :description, command = :command,
                cron = :cron, timeout = :timeout, retries = :retries,
                retry_delay = :retryDelay, status = :status, priority = :priority,
                start_time = :startTime, end_time = :endTime, workflow_id = :workflowId,
                sequence = :sequence, parameters = :parameters, notification = :notification,
                updated_by = :updatedBy, updated_time = :updatedTime
            WHERE id = :id AND tenant_id = :tenantId
        """;

        MapSqlParameterSource params = createParameterSource(task);
        namedParameterJdbcTemplate.update(sql, params);
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = "SELECT * FROM fj_task WHERE id = ? AND tenant_id = ?";
        List<Task> tasks = jdbcTemplate.query(sql, rowMapper, id, getCurrentTenantId());
        return tasks.isEmpty() ? Optional.empty() : Optional.of(tasks.get(0));
    }

    @Override
    public List<Task> findByWorkflowId(Long workflowId) {
        String sql = """
            SELECT * FROM fj_task 
            WHERE workflow_id = ? AND tenant_id = ?
            ORDER BY sequence
        """;
        return jdbcTemplate.query(sql, rowMapper, workflowId, getCurrentTenantId());
    }

    @Override
    public List<Task> findScheduledTasks(LocalDateTime now) {
        String sql = """
            SELECT * FROM fj_task 
            WHERE tenant_id = ? 
            AND status = 'SCHEDULED'
            AND (start_time IS NULL OR start_time <= ?)
            AND (end_time IS NULL OR end_time > ?)
        """;
        return jdbcTemplate.query(sql, rowMapper, getCurrentTenantId(), now, now);
    }

    private MapSqlParameterSource createParameterSource(Task task) {
        return new MapSqlParameterSource()
            .addValue("id", task.getId())
            .addValue("name", task.getName())
            .addValue("description", task.getDescription())
            .addValue("command", task.getCommand())
            .addValue("cron", task.getCron())
            .addValue("timeout", task.getTimeout())
            .addValue("retries", task.getRetries())
            .addValue("retryDelay", task.getRetryDelay())
            .addValue("status", task.getStatus().name())
            .addValue("priority", task.getPriority() != null ? task.getPriority().name() : null)
            .addValue("startTime", task.getStartTime())
            .addValue("endTime", task.getEndTime())
            .addValue("workflowId", task.getWorkflowId())
            .addValue("sequence", task.getSequence())
            .addValue("parameters", task.getParameters())
            .addValue("notification", task.getNotification())
            .addValue("tenantId", task.getTenantId())
            .addValue("createdBy", task.getCreatedBy())
            .addValue("createdTime", task.getCreatedTime())
            .addValue("updatedBy", task.getUpdatedBy())
            .addValue("updatedTime", task.getUpdatedTime());
    }

    private static class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = new Task();
            task.setId(rs.getLong("id"));
            task.setName(rs.getString("name"));
            task.setDescription(rs.getString("description"));
            task.setCommand(rs.getString("command"));
            task.setCron(rs.getString("cron"));
            task.setTimeout(rs.getInt("timeout"));
            task.setRetries(rs.getInt("retries"));
            task.setRetryDelay(rs.getInt("retry_delay"));
            task.setStatus(Task.TaskStatus.valueOf(rs.getString("status")));
            
            String priority = rs.getString("priority");
            if (priority != null) {
                task.setPriority(Task.TaskPriority.valueOf(priority));
            }
            
            if (rs.getTimestamp("start_time") != null) {
                task.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
            }
            
            if (rs.getTimestamp("end_time") != null) {
                task.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
            
            task.setWorkflowId(rs.getLong("workflow_id"));
            task.setSequence(rs.getInt("sequence"));
            task.setParameters(rs.getString("parameters"));
            task.setNotification(rs.getString("notification"));
            task.setTenantId(rs.getLong("tenant_id"));
            task.setCreatedBy(rs.getString("created_by"));
            
            if (rs.getTimestamp("created_time") != null) {
                task.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
            }
            
            task.setUpdatedBy(rs.getString("updated_by"));
            
            if (rs.getTimestamp("updated_time") != null) {
                task.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
            }
            
            return task;
        }
    }

    private Long getCurrentTenantId() {
        // Implement based on your tenant management system
        return 1L;
    }
}
