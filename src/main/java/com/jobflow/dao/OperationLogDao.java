package com.jobflow.dao;

import com.jobflow.domain.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
public class OperationLogDao extends BaseDao<OperationLog> {

    private static final String TABLE_NAME = "fj_operation_log";

    private static final RowMapper<OperationLog> ROW_MAPPER = (rs, rowNum) -> {
        OperationLog operationLog = new OperationLog();
        operationLog.setId(rs.getLong("id"));
        operationLog.setTenantId(rs.getLong("tenant_id"));
        operationLog.setOperationId(rs.getString("operation_id"));
        operationLog.setType(OperationLog.OperationType.valueOf(rs.getString("type")));
        operationLog.setModule(OperationLog.OperationModule.valueOf(rs.getString("module")));
        operationLog.setStatus(OperationLog.OperationStatus.valueOf(rs.getString("status")));
        operationLog.setOperatorId(rs.getLong("operator_id"));
        operationLog.setOperatorName(rs.getString("operator_name"));
        operationLog.setResourceId(rs.getString("resource_id"));
        operationLog.setResourceType(rs.getString("resource_type"));
        operationLog.setResourceName(rs.getString("resource_name"));
        operationLog.setOperation(rs.getString("operation"));
        operationLog.setParameters(rs.getString("parameters"));
        operationLog.setResult(rs.getString("result"));
        operationLog.setClientIp(rs.getString("client_ip"));
        operationLog.setUserAgent(rs.getString("user_agent"));
        operationLog.setDuration(rs.getLong("duration"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            operationLog.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            operationLog.setEndTime(endTime.toLocalDateTime());
        }
        
        operationLog.setCreatedBy(rs.getString("created_by"));
        
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            operationLog.setCreatedTime(createdTime.toLocalDateTime());
        }
        
        operationLog.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            operationLog.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        
        operationLog.setDeleted(rs.getBoolean("deleted"));
        operationLog.setVersion(rs.getInt("version"));
        
        return operationLog;
    };

    public OperationLogDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, TABLE_NAME, ROW_MAPPER);
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO " + TABLE_NAME + " (tenant_id, operation_id, type, module, status, " +
               "operator_id, operator_name, resource_id, resource_type, resource_name, operation, " +
               "parameters, result, client_ip, user_agent, duration, start_time, end_time, " +
               "created_by, created_time, updated_by, updated_time, deleted, version) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, OperationLog log) throws SQLException {
        int i = 1;
        ps.setLong(i++, log.getTenantId());
        ps.setString(i++, log.getOperationId());
        ps.setString(i++, log.getType().name());
        ps.setString(i++, log.getModule().name());
        ps.setString(i++, log.getStatus().name());
        ps.setLong(i++, log.getOperatorId());
        ps.setString(i++, log.getOperatorName());
        ps.setString(i++, log.getResourceId());
        ps.setString(i++, log.getResourceType());
        ps.setString(i++, log.getResourceName());
        ps.setString(i++, log.getOperation());
        ps.setString(i++, log.getParameters());
        ps.setString(i++, log.getResult());
        ps.setString(i++, log.getClientIp());
        ps.setString(i++, log.getUserAgent());
        ps.setLong(i++, log.getDuration());
        ps.setTimestamp(i++, Timestamp.valueOf(log.getStartTime()));
        ps.setTimestamp(i++, log.getEndTime() != null ? Timestamp.valueOf(log.getEndTime()) : null);
        ps.setString(i++, log.getCreatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(log.getCreatedTime()));
        ps.setString(i++, log.getUpdatedBy());
        ps.setTimestamp(i++, Timestamp.valueOf(log.getUpdatedTime()));
        ps.setBoolean(i++, log.getDeleted());
        ps.setInt(i, log.getVersion());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE " + TABLE_NAME + " SET status = ?, result = ?, duration = ?, " +
               "end_time = ?, updated_by = ?, updated_time = ?, version = version + 1 " +
               "WHERE id = ? AND version = ? AND deleted = false";
    }

    @Override
    protected Object[] getUpdateParameters(OperationLog log) {
        return new Object[]{
            log.getStatus().name(),
            log.getResult(),
            log.getDuration(),
            log.getEndTime() != null ? Timestamp.valueOf(log.getEndTime()) : null,
            log.getUpdatedBy(),
            Timestamp.valueOf(log.getUpdatedTime()),
            log.getId(),
            log.getVersion()
        };
    }

    /**
     * Find logs by time range
     */
    public List<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, 
                                            Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE tenant_id = ? AND start_time >= ? AND start_time <= ? " +
                    " AND deleted = false ORDER BY start_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, tenantId, 
                                    Timestamp.valueOf(startTime), 
                                    Timestamp.valueOf(endTime));
        } catch (DataAccessException e) {
            log.error("Failed to find logs by time range: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find logs by operator
     */
    public List<OperationLog> findByOperator(Long operatorId, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE operator_id = ? AND tenant_id = ? AND deleted = false " +
                    " ORDER BY start_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, operatorId, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find logs by operator: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find logs by module and resource
     */
    public List<OperationLog> findByModuleAndResource(OperationLog.OperationModule module, 
                                                     String resourceId, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE module = ? AND resource_id = ? AND tenant_id = ? " +
                    " AND deleted = false ORDER BY start_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, module.name(), resourceId, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find logs by module and resource: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find logs by operation type
     */
    public List<OperationLog> findByType(OperationLog.OperationType type, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE type = ? AND tenant_id = ? AND deleted = false " +
                    " ORDER BY start_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, type.name(), tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find logs by type: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find logs by status
     */
    public List<OperationLog> findByStatus(OperationLog.OperationStatus status, Long tenantId) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
                    " WHERE status = ? AND tenant_id = ? AND deleted = false " +
                    " ORDER BY start_time DESC";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, status.name(), tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find logs by status: {}", e.getMessage());
            throw e;
        }
    }
}
