package com.jobflow.service;

import com.jobflow.dao.BaseDao;
import com.jobflow.dao.OperationLogDao;
import com.jobflow.domain.BaseEntity;
import com.jobflow.domain.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
public abstract class AbstractBaseService<T extends BaseEntity> implements BaseService<T> {

    protected final BaseDao<T> baseDao;
    protected final OperationLogDao operationLogDao;

    @Autowired
    protected AbstractBaseService(BaseDao<T> baseDao, OperationLogDao operationLogDao) {
        this.baseDao = baseDao;
        this.operationLogDao = operationLogDao;
    }

    /**
     * Get the operation module type for logging
     */
    protected abstract OperationLog.OperationModule getOperationModule();

    /**
     * Get entity name for logging
     */
    protected abstract String getEntityName();

    @Override
    @Transactional
    public T create(T entity, String operator) {
        OperationLog operationLog = createOperationLog(
            OperationLog.OperationType.CREATE,
            operator,
            null,
            entity.toString()
        );

        try {
            Long id = baseDao.insert(entity, operator);
            entity.setId(id);
            
            operationLog.markAsSuccess("Created " + getEntityName() + " with ID: " + id);
            operationLogDao.insert(operationLog, operator);
            
            return entity;
        } catch (Exception e) {
            log.error("Failed to create {}: {}", getEntityName(), e.getMessage());
            operationLog.markAsFailed("Failed to create " + getEntityName() + ": " + e.getMessage());
            operationLogDao.insert(operationLog, operator);
            throw e;
        }
    }

    @Override
    @Transactional
    public T update(T entity, String operator) {
        OperationLog operationLog = createOperationLog(
            OperationLog.OperationType.UPDATE,
            operator,
            entity.getId().toString(),
            entity.toString()
        );

        try {
            boolean updated = baseDao.update(entity, operator);
            if (!updated) {
                throw new RuntimeException(getEntityName() + " not found or version mismatch");
            }
            
            operationLog.markAsSuccess("Updated " + getEntityName() + " with ID: " + entity.getId());
            operationLogDao.insert(operationLog, operator);
            
            return entity;
        } catch (Exception e) {
            log.error("Failed to update {}: {}", getEntityName(), e.getMessage());
            operationLog.markAsFailed("Failed to update " + getEntityName() + ": " + e.getMessage());
            operationLogDao.insert(operationLog, operator);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean delete(Long id, String operator) {
        OperationLog operationLog = createOperationLog(
            OperationLog.OperationType.DELETE,
            operator,
            id.toString(),
            "Deleting " + getEntityName() + " with ID: " + id
        );

        try {
            boolean deleted = baseDao.delete(id, operator);
            if (!deleted) {
                throw new RuntimeException(getEntityName() + " not found");
            }
            
            operationLog.markAsSuccess("Deleted " + getEntityName() + " with ID: " + id);
            operationLogDao.insert(operationLog, operator);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to delete {}: {}", getEntityName(), e.getMessage());
            operationLog.markAsFailed("Failed to delete " + getEntityName() + ": " + e.getMessage());
            operationLogDao.insert(operationLog, operator);
            throw e;
        }
    }

    @Override
    public T findById(Long id) {
        try {
            return baseDao.findById(id);
        } catch (Exception e) {
            log.error("Failed to find {} by ID: {}", getEntityName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public List<T> findByTenantId(Long tenantId) {
        try {
            return baseDao.findByTenantId(tenantId);
        } catch (Exception e) {
            log.error("Failed to find {} by tenant ID: {}", getEntityName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return baseDao.findAll();
        } catch (Exception e) {
            log.error("Failed to find all {}: {}", getEntityName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean exists(Long id) {
        try {
            return baseDao.exists(id);
        } catch (Exception e) {
            log.error("Failed to check {} existence: {}", getEntityName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public long count() {
        try {
            return baseDao.count();
        } catch (Exception e) {
            log.error("Failed to count {}: {}", getEntityName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Create operation log entry
     */
    protected OperationLog createOperationLog(OperationLog.OperationType type, 
                                            String operator,
                                            String resourceId,
                                            String parameters) {
        return OperationLog.builder()
            .type(type)
            .module(getOperationModule())
            .operatorName(operator)
            .resourceType(getEntityName())
            .resourceId(resourceId)
            .parameters(parameters)
            .build();
    }

    /**
     * Log operation with custom details
     */
    protected void logOperation(String operator, 
                              OperationLog.OperationType type,
                              String resourceId,
                              String operation,
                              String parameters) {
        OperationLog operationLog = createOperationLog(type, operator, resourceId, parameters);
        operationLog.setOperation(operation);
        operationLogDao.insert(operationLog, operator);
    }
}
