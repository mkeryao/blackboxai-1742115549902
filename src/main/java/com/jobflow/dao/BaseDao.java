package com.jobflow.dao;

import com.jobflow.domain.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class BaseDao<T extends BaseEntity> {
    
    protected final JdbcTemplate jdbcTemplate;
    protected final String tableName;
    protected final RowMapper<T> rowMapper;

    protected BaseDao(JdbcTemplate jdbcTemplate, String tableName, RowMapper<T> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.rowMapper = rowMapper;
    }

    /**
     * Insert an entity
     */
    public Long insert(T entity, String operator) {
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.prePersist();

        String sql = getInsertSql();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                setInsertParameters(ps, entity);
                return ps;
            }, keyHolder);

            return Objects.requireNonNull(keyHolder.getKey()).longValue();
        } catch (DataAccessException e) {
            log.error("Failed to insert entity to {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Update an entity
     */
    public boolean update(T entity, String operator) {
        entity.setUpdatedBy(operator);
        entity.preUpdate();

        String sql = getUpdateSql();
        try {
            int rows = jdbcTemplate.update(sql, getUpdateParameters(entity));
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to update entity in {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Soft delete an entity
     */
    public boolean delete(Long id, String operator) {
        String sql = String.format(
            "UPDATE %s SET deleted = true, updated_by = ?, updated_time = ? WHERE id = ? AND deleted = false",
            tableName
        );
        try {
            int rows = jdbcTemplate.update(sql, operator, Timestamp.valueOf(LocalDateTime.now()), id);
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("Failed to delete entity from {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Find entity by ID
     */
    public T findById(Long id) {
        String sql = String.format(
            "SELECT * FROM %s WHERE id = ? AND deleted = false",
            tableName
        );
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, id);
        } catch (DataAccessException e) {
            log.error("Failed to find entity by ID from {}: {}", tableName, e.getMessage());
            return null;
        }
    }

    /**
     * Find all entities by tenant ID
     */
    public List<T> findByTenantId(Long tenantId) {
        String sql = String.format(
            "SELECT * FROM %s WHERE tenant_id = ? AND deleted = false ORDER BY id DESC",
            tableName
        );
        try {
            return jdbcTemplate.query(sql, rowMapper, tenantId);
        } catch (DataAccessException e) {
            log.error("Failed to find entities by tenant ID from {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Find all entities
     */
    public List<T> findAll() {
        String sql = String.format(
            "SELECT * FROM %s WHERE deleted = false ORDER BY id DESC",
            tableName
        );
        try {
            return jdbcTemplate.query(sql, rowMapper);
        } catch (DataAccessException e) {
            log.error("Failed to find all entities from {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Count total records
     */
    public long count() {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE deleted = false",
            tableName
        );
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (DataAccessException e) {
            log.error("Failed to count entities from {}: {}", tableName, e.getMessage());
            throw e;
        }
    }

    /**
     * Check if entity exists
     */
    public boolean exists(Long id) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE id = ? AND deleted = false",
            tableName
        );
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.error("Failed to check entity existence in {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Get insert SQL
     */
    protected abstract String getInsertSql();

    /**
     * Get update SQL
     */
    protected abstract String getUpdateSql();

    /**
     * Set parameters for insert
     */
    protected abstract void setInsertParameters(PreparedStatement ps, T entity) throws java.sql.SQLException;

    /**
     * Get parameters for update
     */
    protected abstract Object[] getUpdateParameters(T entity);
}
