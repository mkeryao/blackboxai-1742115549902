package com.jobflow.dao;

import com.jobflow.domain.Task;
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
public class TaskDao extends BaseDao<Task> {

    private static final String TABLE_NAME = "fj_task";

    private static final RowMapper<Task> ROW_MAPPER = (rs, rowNum) -> {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTenantId(rs.getLong("tenant_id"));
        task.setName(rs.getString("name"));
        task.setDescription(rs.getString("description"));
        task.setGroupName(rs.getString("group_name"));
        task.setType(Task.TaskType.valueOf(rs.getString("type")));
        task.setStatus(Task.TaskStatus.valueOf(rs.getString("status")));
        task.setCommand(rs.getString("command"));
        task.setUrl(rs.getString("url"));
        task.setBeanName(rs.getString("bean_name"));
        task.setMethodName(rs.getString("method_name"));
        task.setParams(rs.getString("params"));
        task.setCronExpression(rs.getString("cron_expression"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            task.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            task.setEndTime(endTime.toLocalDateTime());
        }
        
        task.setWorkCalendar(rs.getString("work_calendar"));
        task.setMaxRetries(rs.getInt("max_retries"));
        task.setCurrentRetries(rs.getInt("current_retries"));
        task.setRetryInterval(rs.getLong("retry_interval"));
        task.setTimeout(rs.getLong("timeout"));
        task.setNotifyUsers(rs.getString("notify_users"));
        task.setNotifyOnSuccess(rs.getBoolean("notify_on_success"));
        task.setNotifyOnFailure(rs.getBoolean("notify_on_failure"));
        
        Timestamp lastExecTime = rs.getTimestamp("last_execution_time");
        if (lastExecTime != null) {
            task.setLastExecutionTime(lastExecTime.toLocalDateTime());
        }
        
        Timestamp nextExecTime = rs.getTimestamp("next_execution_time");
        if (nextExecTime != null) {
            task.setNextExecutionTime(nextExecTime.toLocalDateTime());
        }
        
        task.setLastExecutionResult(rs.getString("last_execution_result"));
        task.setLastExecutionDuration(rs.getLong("last_execution_duration"));
        task.setPriority(rs.getInt("priority"));
        task.setEnabled(rs.getBoolean("enabled"));
        task.setCreatedBy(rs.getString("created_by"));
        
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            task.setCreatedTime(createdTime.toLocalDateTime());
        }
        
        task.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            task.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        
        task.setDeleted(rs.getBoolean("deleted"));
        task.setVersion(rs.getInt("version"));
        
        return task;
    };

    public TaskDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, TABLE_NAME, ROW_MAPPER);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + TABLE_NAME + " (tenant_id, name, description, group_name, type, status, " +
               "command, url, bean_name, method_name, params, cron_expression, start_time, end_time, " +
               "work_calendar, max_retries, current_retries, retry_interval, timeout, notify_users, " +
               "notify_on_success, notify_on_failure, last_execution_time, next_execution_time, " +
               "last_execution_result, last_execution_duration, priority, enabled, created_by, " +
               "created_time, updated_by, updated_time, deleted, version) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Task task) throws SQLException {
        int i = 1;
        ps.setLong(i++, task.getTenantId());
        ps.setString(i++, task.getName());
        ps.setString(i++, task.getDescription());
        ps.setString(i++, task.getGroupName());
        ps.setString(i++, task.getType().name());
        ps.setString(i++, task.getStatus().name());
        ps.setString(i++, task.getCommand());
        ps.setString(i++, task.getUrl());
        ps.setString(i++, task.getBeanName());
        ps.setString(i++, task.getMethodName());
        ps.setString(i++, task.getParams());
        ps.setString(i++, task.getCronExpression());
        ps.setTimestamp(i++, task.getStartTime() != null ? Timestamp.valueOf(task.getStartTime()) : null);
        ps.setTimestamp(i++, task.getEndTime() != null ? Timestamp.valueOf(task.getEndTime()) : null);
        ps.setString(i++, task.getWorkCalendar());
        ps.setInt(i++, task.getMaxRetries());
        ps.setInt(i++, task.getCurrentRetries());
        ps.setLong(i++, task.getRetryInterval());
        ps.setLong(i++, task.getTimeout());
        ps.setString(i++, task.getNotifyUsers());
        ps.setBoolean(i++, task.getNotifyOnSuccess());
        ps.setBoolean(i++, task.getNotifyOnFailure());
        ps.setTimestamp(i++, task.getLastExecutionTime() != null ? Timestamp.valueOf(task.getLastExecutionTime()) : null);
        ps.setTimestamp(i++, task.getNextExecutionTime() != null ? Timestamp.valueOf(task.getNextExecutionTime()) : null);
        ps.setString(i++, task.getLastExecutionResult());
        ps.setLong(i++, task.getLastExecutionDuration() != null ? task.getLastExecutionDuration() : 0);
        ps.setInt(i++, task.getPriority());
        ps.setBoolean(i++, task.getEnabled());
        ps.setString(i++, task.getCreatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(task.getCreatedTime()));
        ps.setString(i++, task.getUpdatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(task.getUpdatedTime()));
        ps.setBoolean(i++, task.getDeleted());
        ps.setInt(i, task.getVersion());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE " + TABLE_NAME + " SET name = ?, description = ?, group_name = ?, type = ?, " +
               "status = ?, command = ?, url = ?, bean_name = ?, method_name = ?, params = ?, " +
               "cron_expression = ?, start_time = ?, end_time = ?, work_calendar = ?, max_retries = ?, " +
               "current_retries = ?, retry_interval = ?, timeout = ?, notify_users = ?, " +
               "notify_on_success = ?, notify_on_failure = ?, last_execution_time = ?, " +
               "next_execution_time = ?, last_execution_result = ?, last_execution_duration = ?, " +
               "priority = ?, enabled = ?, updated_by = ?, updated_time = ?, version = version + 1 " +
               "WHERE id = ? AND version = ? AND deleted = false";
    }

    @Override
    protected Object[] getUpdateParameters(Task task) {
        return new Object[]{
            task.getName(),
            task.getDescription(),
            task.getGroupName(),
            task.getType().name(),
            task.getStatus().name(),
            task.getCommand(),
            task.getUrl(),
            task.getBeanName(),
            task.getMethodName(),
            task.getParams(),
            task.getCronExpression(),
            task.getStartTime() != null ? Timestamp.valueOf(task.getStartTime()) : null,
            task.getEndTime() != null ? Timestamp.valueOf(task.getEndTime()) : null,
            task.getWorkCalendar(),
            task.getMaxRetries(),
            task.getCurrentRetries(),
            task.getRetryInterval(),
            task.getTimeout(),
            task.getNotifyUsers(),
            task.getNotifyOnSuccess(),
            task.getNotifyOnFailure(),
            task.getLastExecutionTime() != null ? Timestamp.valueOf(task.getLastExecutionTime()) : null,
            task.getNextExecutionTime() != null ? Timestamp.valueOf(task.getNextExecutionTime()) : null,
            task.getLastExecutionResult(),
            task.getLastExecutionDuration(),
            task.getPriority(),
            task.getEnabled(),
            task.getUpdatedBy(),
            Timestamp.valueOf(task.getUpdatedTime()),
            task.getId(),
            task.getVersion()
        };
    }

    /**
     * Find tasks by group name
     */
    public List<Task> findByGroupName(String groupName, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE group_name = ? AND tenant_id = ? AND deleted = false ORDER BY priority ASC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, groupName, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find tasks by group name: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find tasks that are due for execution
     */
    public List<Task> findDueTasks(Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE tenant_id = ? AND enabled = true AND deleted = false " +
                    " AND (next_execution_time IS NULL OR next_execution_time <= NOW()) " +
                    " AND (start_time IS NULL OR start_time <= NOW()) " +
                    " AND (end_time IS NULL OR end_time >= NOW()) " +
                    " AND status NOT IN ('RUNNING', 'PENDING') " +
                    " ORDER BY priority ASC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find due tasks: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update task status
     */
    public boolean updateStatus(Long id, Task.TaskStatus status, String operator) {
        String sql = "UPDATE " + TABLE_NAME + 
                    " SET status = ?, updated_by = ?, updated_time = ?, version = version + 1 " +
                    " WHERE id = ? AND deleted = false";
        try {
            int rows = jdbcTemplate.update(sql, status.name(), operator, 
                                         Timestamp.valueOf(java.time.LocalDateTime.now()), id);
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to update task status: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find task by name and tenant
     */
    public Task findByName(String name, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE name = ? AND tenant_id = ? AND deleted = false";
        try {
            return jdbcTemplate.queryForObject(sql, ROW_MAPPER, name, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find task by name: {}", e.getMessage());
            return null;
        }
    }
}
