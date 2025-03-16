package com.jobflow.service;

import com.jobflow.domain.BaseEntity;
import java.util.List;

/**
 * Base service interface defining common CRUD operations
 * @param <T> Entity type extending BaseEntity
 */
public interface BaseService<T extends BaseEntity> {
    
    /**
     * Create a new entity
     * @param entity Entity to create
     * @param operator User performing the operation
     * @return Created entity with ID
     */
    T create(T entity, String operator);

    /**
     * Update an existing entity
     * @param entity Entity to update
     * @param operator User performing the operation
     * @return Updated entity
     */
    T update(T entity, String operator);

    /**
     * Delete an entity by ID
     * @param id Entity ID
     * @param operator User performing the operation
     * @return true if deleted successfully
     */
    boolean delete(Long id, String operator);

    /**
     * Find entity by ID
     * @param id Entity ID
     * @return Entity if found, null otherwise
     */
    T findById(Long id);

    /**
     * Find all entities by tenant ID
     * @param tenantId Tenant ID
     * @return List of entities
     */
    List<T> findByTenantId(Long tenantId);

    /**
     * Find all entities
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Check if entity exists
     * @param id Entity ID
     * @return true if exists
     */
    boolean exists(Long id);

    /**
     * Count total number of entities
     * @return Total count
     */
    long count();
}
