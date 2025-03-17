package com.jobflow.dao.jdbc;

import com.jobflow.dao.ExecutionRecordDao;
import com.jobflow.domain.ExecutionRecord;
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
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExecutionRecordJdbcDao implements ExecutionRecordDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<ExecutionRecord> rowMapper = new ExecutionRecordRowMapper();

    @Override
    public ExecutionRecord save(ExecutionRecord record) {
        if (record.getId() == null) {
            return insert(record);
        } else {
            return update(record);
        }
    }

    private ExecutionRecord insert(ExecutionRecord record) {
        String sql = """
            INSERT INTO fj_execution_record (
                execution_id, type, task_id, workflow_id, status, start_time, end_time,
                duration, error_message, stack_trace, input_params, output_result,
                retry_count, max_retries, next_retry_time, executor, executor_ip,
                trigger_type, trigger_info, environment, resource_usage, tenant_id,
                created_by, created_time, updated_by, updated_time
            ) VALUES (
                :executionId, :type, :taskId, :workflowId, :status, :startTime, :endTime,
                :duration, :errorMessage, :stackTrace, :inputParams, :outputResult,
                :retryCount, :maxRetries, :nextRetryTime, :executor, :executorIp,
                :triggerType, :triggerInfo, :environment, :resourceUsage, :tenantId,
                :createdBy, :createdTime, :updatedBy, :updatedTime
            )
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParameterSource(record);

        namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        record.setId(keyHolder.getKey().longValue());
        return record;
    }

    private ExecutionRecord update(ExecutionRecord record) {
        String sql = """
            UPDATE fj_execution_record SET
                type = :type, task_id = :taskId, workflow_id = :workflowId,
                status = :status, start_time = :startTime, end_time = :endTime,
                duration = :duration, error_message = :errorMessage,
                stack_trace = :stackTrace, input_params = :inputParams,
                output_result = :outputResult, retry_count = :retryCount,
                max_retries = :maxRetries, next_retry_time = :nextRetryTime,
                executor = :executor, executor_ip = :executorIp,
                trigger_type = :triggerType, trigger_info = :triggerInfo,
                environment = :environment, resource_usage = :resourceUsage,
                updated_by = :updatedBy, updated_time = :updatedTime
            WHERE id = :id AND tenant_id = :tenantId
        """;

        MapSqlParameterSource params = createParameterSource(record);
        namedParameterJdbcTemplate.update(sql, params);
        return record;
    }

    @Override
    public Optional<ExecutionRecord> findById(Long id) {
        String sql = "SELECT * FROM fj_execution_record WHERE id = ? AND tenant_id = ?";
        List<ExecutionRecord> records = jdbcTemplate.query(sql, rowMapper, id, getCurrentTenantId());
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    @Override
    public List<ExecutionRecord> findTaskExecutions(Long tenantId, ExecutionRecord.ExecutionType type, Long taskId) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = ? AND type = ? AND task_id = ?
            ORDER BY start_time DESC
        """;
        return jdbcTemplate.query(sql, rowMapper, tenantId, type.name(), taskId);
    }

    @Override
    public List<ExecutionRecord> findWorkflowExecutions(Long tenantId, ExecutionRecord.ExecutionType type, Long workflowId) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = ? AND type = ? AND workflow_id = ?
            ORDER BY start_time DESC
        """;
        return jdbcTemplate.query(sql, rowMapper, tenantId, type.name(), workflowId);
    }

    @Override
    public List<ExecutionRecord> findRetryableExecutions(Long tenantId, ExecutionRecord.ExecutionStatus status, LocalDateTime now) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = ? AND status = ? AND next_retry_time <= ?
        """;
        return jdbcTemplate.query(sql, rowMapper, tenantId, status.name(), now);
    }

    @Override
    public List<ExecutionRecord> findByStatusAndStartTime(Long tenantId, List<ExecutionRecord.ExecutionStatus> statuses, LocalDateTime startTime) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = :tenantId 
            AND status IN (:statuses) 
            AND start_time >= :startTime
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("statuses", statuses.stream().map(Enum::name).toList())
            .addValue("startTime", startTime);

        return namedParameterJdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    public List<Object[]> getExecutionStatistics(Long tenantId, LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT status, COUNT(*) as count 
            FROM fj_execution_record 
            WHERE tenant_id = ? 
            AND start_time BETWEEN ? AND ?
            GROUP BY status
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
            ExecutionRecord.ExecutionStatus.valueOf(rs.getString("status")),
            rs.getLong("count")
        }, tenantId, start, end);
    }

    @Override
    public Double getAverageExecutionTime(Long tenantId, ExecutionRecord.ExecutionType type, LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT AVG(duration) 
            FROM fj_execution_record 
            WHERE tenant_id = ? 
            AND type = ? 
            AND status = 'COMPLETED'
            AND start_time BETWEEN ? AND ?
        """;
        return jdbcTemplate.queryForObject(sql, Double.class, tenantId, type.name(), start, end);
    }

    @Override
    public List<ExecutionRecord> findTimedOutExecutions(Long tenantId, LocalDateTime timeout) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = ? 
            AND status = 'RUNNING' 
            AND start_time <= ?
        """;
        return jdbcTemplate.query(sql, rowMapper, tenantId, timeout);
    }

    @Override
    public ExecutionRecord findByExecutionId(Long tenantId, String executionId) {
        String sql = """
            SELECT * FROM fj_execution_record 
            WHERE tenant_id = ? AND execution_id = ?
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, tenantId, executionId);
    }

    private MapSqlParameterSource createParameterSource(ExecutionRecord record) {
        return new MapSqlParameterSource()
            .addValue("id", record.getId())
            .addValue("executionId", record.getExecutionId())
            .addValue("type", record.getType().name())
            .addValue("taskId", record.getTask() != null ? record.getTask().getId() : null)
            .addValue("workflowId", record.getWorkflow() != null ? record.getWorkflow().getId() : null)
            .addValue("status", record.getStatus().name())
            .addValue("startTime", record.getStartTime())
            .addValue("endTime", record.getEndTime())
            .addValue("duration", record.getDuration())
            .addValue("errorMessage", record.getErrorMessage())
            .addValue("stackTrace", record.getStackTrace())
            .addValue("inputParams", record.getInputParams())
            .addValue("outputResult", record.getOutputResult())
            .addValue("retryCount", record.getRetryCount())
            .addValue("maxRetries", record.getMaxRetries())
            .addValue("nextRetryTime", record.getNextRetryTime())
            .addValue("executor", record.getExecutor())
            .addValue("executorIp", record.getExecutorIp())
            .addValue("triggerType", record.getTriggerType() != null ? record.getTriggerType().name() : null)
            .addValue("triggerInfo", record.getTriggerInfo())
            .addValue("environment", record.getEnvironment())
            .addValue("resourceUsage", record.getResourceUsage())
            .addValue("tenantId", record.getTenantId())
            .addValue("createdBy", record.getCreatedBy())
            .addValue("createdTime", record.getCreatedTime())
            .addValue("updatedBy", record.getUpdatedBy())
            .addValue("updatedTime", record.getUpdatedTime());
    }

    private static class ExecutionRecordRowMapper implements RowMapper<ExecutionRecord> {
        @Override
        public ExecutionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExecutionRecord record = new ExecutionRecord();
            record.setId(rs.getLong("id"));
            record.setExecutionId(rs.getString("execution_id"));
            record.setType(ExecutionRecord.ExecutionType.valueOf(rs.getString("type")));
            record.setStatus(ExecutionRecord.ExecutionStatus.valueOf(rs.getString("status")));
            record.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
            
            if (rs.getTimestamp("end_time") != null) {
                record.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
            
            record.setDuration(rs.getLong("duration"));
            record.setErrorMessage(rs.getString("error_message"));
            record.setStackTrace(rs.getString("stack_trace"));
            record.setInputParams(rs.getString("input_params"));
            record.setOutputResult(rs.getString("output_result"));
            record.setRetryCount(rs.getInt("retry_count"));
            record.setMaxRetries(rs.getInt("max_retries"));
            
            if (rs.getTimestamp("next_retry_time") != null) {
                record.setNextRetryTime(rs.getTimestamp("next_retry_time").toLocalDateTime());
            }
            
            record.setExecutor(rs.getString("executor"));
            record.setExecutorIp(rs.getString("executor_ip"));
            
            String triggerType = rs.getString("trigger_type");
            if (triggerType != null) {
                record.setTriggerType(ExecutionRecord.TriggerType.valueOf(triggerType));
            }
            
            record.setTriggerInfo(rs.getString("trigger_info"));
            record.setEnvironment(rs.getString("environment"));
            record.setResourceUsage(rs.getString("resource_usage"));
            record.setTenantId(rs.getLong("tenant_id"));
            record.setCreatedBy(rs.getString("created_by"));
            
            if (rs.getTimestamp("created_time") != null) {
                record.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
            }
            
            record.setUpdatedBy(rs.getString("updated_by"));
            
            if (rs.getTimestamp("updated_time") != null) {
                record.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
            }
            
            return record;
        }
    }

    private Long getCurrentTenantId() {
        // Implement based on your tenant management system
        return 1L;
    }
}
