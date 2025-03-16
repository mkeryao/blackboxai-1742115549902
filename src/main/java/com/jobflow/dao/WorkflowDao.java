package com.jobflow.dao;

import com.jobflow.domain.Workflow;
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
public class WorkflowDao extends BaseDao<Workflow> {

    private static final String TABLE_NAME = "fj_workflow";

    private static final RowMapper<Workflow> ROW_MAPPER = (rs, rowNum) -> {
        Workflow workflow = new Workflow();
        workflow.setId(rs.getLong("id"));
        workflow.setTenantId(rs.getLong("tenant_id"));
        workflow.setName(rs.getString("name"));
        workflow.setDescription(rs.getString("description"));
        workflow.setStatus(Workflow.WorkflowStatus.valueOf(rs.getString("status")));
        workflow.setCronExpression(rs.getString("cron_expression"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            workflow.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            workflow.setEndTime(endTime.toLocalDateTime());
        }
        
        workflow.setWorkCalendar(rs.getString("work_calendar"));
        workflow.setNotifyUsers(rs.getString("notify_users"));
        workflow.setNotifyOnSuccess(rs.getBoolean("notify_on_success"));
        workflow.setNotifyOnFailure(rs.getBoolean("notify_on_failure"));
        
        Timestamp lastExecTime = rs.getTimestamp("last_execution_time");
        if (lastExecTime != null) {
            workflow.setLastExecutionTime(lastExecTime.toLocalDateTime());
        }
        
        Timestamp nextExecTime = rs.getTimestamp("next_execution_time");
        if (nextExecTime != null) {
            workflow.setNextExecutionTime(nextExecTime.toLocalDateTime());
        }
        
        workflow.setLastExecutionResult(rs.getString("last_execution_result"));
        workflow.setLastExecutionDuration(rs.getLong("last_execution_duration"));
        workflow.setDagDefinition(rs.getString("dag_definition"));
        workflow.setEnabled(rs.getBoolean("enabled"));
        workflow.setTimeout(rs.getLong("timeout"));
        workflow.setTotalTasks(rs.getInt("total_tasks"));
        workflow.setCompletedTasks(rs.getInt("completed_tasks"));
        workflow.setFailedTasks(rs.getInt("failed_tasks"));
        workflow.setCreatedBy(rs.getString("created_by"));
        
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            workflow.setCreatedTime(createdTime.toLocalDateTime());
        }
        
        workflow.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            workflow.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        
        workflow.setDeleted(rs.getBoolean("deleted"));
        workflow.setVersion(rs.getInt("version"));
        
        return workflow;
    };

    public WorkflowDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, TABLE_NAME, ROW_MAPPER);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + TABLE_NAME + " (tenant_id, name, description, status, cron_expression, " +
               "start_time, end_time, work_calendar, notify_users, notify_on_success, notify_on_failure, " +
               "last_execution_time, next_execution_time, last_execution_result, last_execution_duration, " +
               "dag_definition, enabled, timeout, total_tasks, completed_tasks, failed_tasks, created_by, " +
               "created_time, updated_by, updated_time, deleted, version) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Workflow workflow) throws SQLException {
        int i = 1;
        ps.setLong(i++, workflow.getTenantId());
        ps.setString(i++, workflow.getName());
        ps.setString(i++, workflow.getDescription());
        ps.setString(i++, workflow.getStatus().name());
        ps.setString(i++, workflow.getCronExpression());
        ps.setTimestamp(i++, workflow.getStartTime() != null ? Timestamp.valueOf(workflow.getStartTime()) : null);
        ps.setTimestamp(i++, workflow.getEndTime() != null ? Timestamp.valueOf(workflow.getEndTime()) : null);
        ps.setString(i++, workflow.getWorkCalendar());
        ps.setString(i++, workflow.getNotifyUsers());
        ps.setBoolean(i++, workflow.getNotifyOnSuccess());
        ps.setBoolean(i++, workflow.getNotifyOnFailure());
        ps.setTimestamp(i++, workflow.getLastExecutionTime() != null ? Timestamp.valueOf(workflow.getLastExecutionTime()) : null);
        ps.setTimestamp(i++, workflow.getNextExecutionTime() != null ? Timestamp.valueOf(workflow.getNextExecutionTime()) : null);
        ps.setString(i++, workflow.getLastExecutionResult());
        ps.setLong(i++, workflow.getLastExecutionDuration() != null ? workflow.getLastExecutionDuration() : 0);
        ps.setString(i++, workflow.getDagDefinition());
        ps.setBoolean(i++, workflow.getEnabled());
        ps.setLong(i++, workflow.getTimeout());
        ps.setInt(i++, workflow.getTotalTasks());
        ps.setInt(i++, workflow.getCompletedTasks());
        ps.setInt(i++, workflow.getFailedTasks());
        ps.setString(i++, workflow.getCreatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(workflow.getCreatedTime()));
        ps.setString(i++, workflow.getUpdatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(workflow.getUpdatedTime()));
        ps.setBoolean(i++, workflow.getDeleted());
        ps.setInt(i, workflow.getVersion());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE " + TABLE_NAME + " SET name = ?, description = ?, status = ?, " +
               "cron_expression = ?, start_time = ?, end_time = ?, work_calendar = ?, " +
               "notify_users = ?, notify_on_success = ?, notify_on_failure = ?, " +
               "last_execution_time = ?, next_execution_time = ?, last_execution_result = ?, " +
               "last_execution_duration = ?, dag_definition = ?, enabled = ?, timeout = ?, " +
               "total_tasks = ?, completed_tasks = ?, failed_tasks = ?, updated_by = ?, " +
               "updated_time = ?, version = version + 1 " +
               "WHERE id = ? AND version = ? AND deleted = false";
    }

    @Override
    protected Object[] getUpdateParameters(Workflow workflow) {
        return new Object[]{
            workflow.getName(),
            workflow.getDescription(),
            workflow.getStatus().name(),
            workflow.getCronExpression(),
            workflow.getStartTime() != null ? Timestamp.valueOf(workflow.getStartTime()) : null,
            workflow.getEndTime() != null ? Timestamp.valueOf(workflow.getEndTime()) : null,
            workflow.getWorkCalendar(),
            workflow.getNotifyUsers(),
            workflow.getNotifyOnSuccess(),
            workflow.getNotifyOnFailure(),
            workflow.getLastExecutionTime() != null ? Timestamp.valueOf(workflow.getLastExecutionTime()) : null,
            workflow.getNextExecutionTime() != null ? Timestamp.valueOf(workflow.getNextExecutionTime()) : null,
            workflow.getLastExecutionResult(),
            workflow.getLastExecutionDuration(),
            workflow.getDagDefinition(),
            workflow.getEnabled(),
            workflow.getTimeout(),
            workflow.getTotalTasks(),
            workflow.getCompletedTasks(),
            workflow.getFailedTasks(),
            workflow.getUpdatedBy(),
            Timestamp.valueOf(workflow.getUpdatedTime()),
            workflow.getId(),
            workflow.getVersion()
        };
    }

    /**
     * Find workflows that are due for execution
     */
    public List<Workflow> findDueWorkflows(Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE tenant_id = ? AND enabled = true AND deleted = false " +
                    " AND (next_execution_time IS NULL OR next_execution_time <= NOW()) " +
                    " AND (start_time IS NULL OR start_time <= NOW()) " +
                    " AND (end_time IS NULL OR end_time >= NOW()) " +
                    " AND status NOT IN ('RUNNING') " +
                    " ORDER BY id ASC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find due workflows: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update workflow status
     */
    public boolean updateStatus(Long id, Workflow.WorkflowStatus status, String operator) {
        String sql = "UPDATE " + TABLE_NAME + 
                    " SET status = ?, updated_by = ?, updated_time = ?, version = version + 1 " +
                    " WHERE id = ? AND deleted = false";
        try {
            int rows = jdbcTemplate.update(sql, status.name(), operator, 
                                         Timestamp.valueOf(java.time.LocalDateTime.now()), id);
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to update workflow status: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find workflow by name and tenant
     */
    public Workflow findByName(String name, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE name = ? AND tenant_id = ? AND deleted = false";
        try {
            return jdbcTemplate.queryForObject(sql, ROW_MAPPER, name, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find workflow by name: {}", e.getMessage());
            return null;
        }
    }
}
