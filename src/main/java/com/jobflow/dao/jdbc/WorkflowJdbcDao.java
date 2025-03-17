package com.jobflow.dao.jdbc;

import com.jobflow.dao.WorkflowDao;
import com.jobflow.domain.Workflow;
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
public class WorkflowJdbcDao implements WorkflowDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<Workflow> rowMapper = new WorkflowRowMapper();

    @Override
    public Workflow save(Workflow workflow) {
        if (workflow.getId() == null) {
            return insert(workflow);
        } else {
            return update(workflow);
        }
    }

    private Workflow insert(Workflow workflow) {
        String sql = """
            INSERT INTO fj_workflow (
                name, description, cron, status, priority, start_time, end_time,
                timeout, retries, retry_delay, notification, parameters,
                concurrent, error_handling, tenant_id, created_by, created_time,
                updated_by, updated_time
            ) VALUES (
                :name, :description, :cron, :status, :priority, :startTime, :endTime,
                :timeout, :retries, :retryDelay, :notification, :parameters,
                :concurrent, :errorHandling, :tenantId, :createdBy, :createdTime,
                :updatedBy, :updatedTime
            )
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParameterSource(workflow);

        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        workflow.setId(keyHolder.getKey().longValue());
        return workflow;
    }

    private Workflow update(Workflow workflow) {
        String sql = """
            UPDATE fj_workflow SET
                name = :name, description = :description, cron = :cron,
                status = :status, priority = :priority, start_time = :startTime,
                end_time = :endTime, timeout = :timeout, retries = :retries,
                retry_delay = :retryDelay, notification = :notification,
                parameters = :parameters, concurrent = :concurrent,
                error_handling = :errorHandling, updated_by = :updatedBy,
                updated_time = :updatedTime
            WHERE id = :id AND tenant_id = :tenantId
        """;

        MapSqlParameterSource params = createParameterSource(workflow);
        namedParameterJdbcTemplate.update(sql, params);
        return workflow;
    }

    @Override
    public Optional<Workflow> findById(Long id) {
        String sql = "SELECT * FROM fj_workflow WHERE id = ? AND tenant_id = ?";
        List<Workflow> workflows = jdbcTemplate.query(sql, rowMapper, id, getCurrentTenantId());
        return workflows.isEmpty() ? Optional.empty() : Optional.of(workflows.get(0));
    }

    @Override
    public List<Workflow> findScheduledWorkflows(LocalDateTime now) {
        String sql = """
            SELECT * FROM fj_workflow 
            WHERE tenant_id = ? 
            AND status = 'SCHEDULED'
            AND (start_time IS NULL OR start_time <= ?)
            AND (end_time IS NULL OR end_time > ?)
        """;
        return jdbcTemplate.query(sql, rowMapper, getCurrentTenantId(), now, now);
    }

    @Override
    public List<Workflow> findDependentWorkflows(Long workflowId) {
        String sql = """
            SELECT w.* FROM fj_workflow w
            INNER JOIN fj_workflow_dependency d ON w.id = d.workflow_id
            WHERE d.dependency_id = ? AND w.tenant_id = ?
        """;
        return jdbcTemplate.query(sql, rowMapper, workflowId, getCurrentTenantId());
    }

    private MapSqlParameterSource createParameterSource(Workflow workflow) {
        return new MapSqlParameterSource()
            .addValue("id", workflow.getId())
            .addValue("name", workflow.getName())
            .addValue("description", workflow.getDescription())
            .addValue("cron", workflow.getCron())
            .addValue("status", workflow.getStatus().name())
            .addValue("priority", workflow.getPriority() != null ? workflow.getPriority().name() : null)
            .addValue("startTime", workflow.getStartTime())
            .addValue("endTime", workflow.getEndTime())
            .addValue("timeout", workflow.getTimeout())
            .addValue("retries", workflow.getRetries())
            .addValue("retryDelay", workflow.getRetryDelay())
            .addValue("notification", workflow.getNotification())
            .addValue("parameters", workflow.getParameters())
            .addValue("concurrent", workflow.getConcurrent())
            .addValue("errorHandling", workflow.getErrorHandling())
            .addValue("tenantId", workflow.getTenantId())
            .addValue("createdBy", workflow.getCreatedBy())
            .addValue("createdTime", workflow.getCreatedTime())
            .addValue("updatedBy", workflow.getUpdatedBy())
            .addValue("updatedTime", workflow.getUpdatedTime());
    }

    private static class WorkflowRowMapper implements RowMapper<Workflow> {
        @Override
        public Workflow mapRow(ResultSet rs, int rowNum) throws SQLException {
            Workflow workflow = new Workflow();
            workflow.setId(rs.getLong("id"));
            workflow.setName(rs.getString("name"));
            workflow.setDescription(rs.getString("description"));
            workflow.setCron(rs.getString("cron"));
            workflow.setStatus(Workflow.WorkflowStatus.valueOf(rs.getString("status")));
            
            String priority = rs.getString("priority");
            if (priority != null) {
                workflow.setPriority(Workflow.WorkflowPriority.valueOf(priority));
            }
            
            if (rs.getTimestamp("start_time") != null) {
                workflow.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
            }
            
            if (rs.getTimestamp("end_time") != null) {
                workflow.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
            
            workflow.setTimeout(rs.getInt("timeout"));
            workflow.setRetries(rs.getInt("retries"));
            workflow.setRetryDelay(rs.getInt("retry_delay"));
            workflow.setNotification(rs.getString("notification"));
            workflow.setParameters(rs.getString("parameters"));
            workflow.setConcurrent(rs.getBoolean("concurrent"));
            workflow.setErrorHandling(rs.getString("error_handling"));
            workflow.setTenantId(rs.getLong("tenant_id"));
            workflow.setCreatedBy(rs.getString("created_by"));
            
            if (rs.getTimestamp("created_time") != null) {
                workflow.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
            }
            
            workflow.setUpdatedBy(rs.getString("updated_by"));
            
            if (rs.getTimestamp("updated_time") != null) {
                workflow.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
            }
            
            return workflow;
        }
    }

    private Long getCurrentTenantId() {
        // Implement based on your tenant management system
        return 1L;
    }
}
