package com.jobflow.controller;

import com.jobflow.domain.User;
import com.jobflow.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base Controller
 * 
 * Provides common functionality and error handling for all controllers.
 */
@Slf4j
public abstract class BaseController {

    /**
     * Get the current authenticated user
     */
    protected User getCurrentUser() {
        return SecurityUtils.getCurrentUser()
            .orElseThrow(() -> new SecurityException("No authenticated user found"));
    }

    /**
     * Get the current user ID
     */
    protected Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new SecurityException("No authenticated user found"));
    }

    /**
     * Get the current tenant ID
     */
    protected Long getCurrentTenantId() {
        return SecurityUtils.getCurrentTenantId()
            .orElseThrow(() -> new SecurityException("No tenant ID found"));
    }

    /**
     * Create success response
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, null));
    }

    /**
     * Create success response with message
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, message));
    }

    /**
     * Create error response
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse<>(false, null, message), status);
    }

    /**
     * Create error response with data
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(T data, String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse<>(false, data, message), status);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());
        return error(errors, "Validation failed", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    protected ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException ex) {
        log.error("Security error: {}", ex.getMessage());
        return error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Check if user has permission to access tenant
     */
    protected boolean canAccessTenant(Long tenantId) {
        return SecurityUtils.canAccessTenant(tenantId);
    }

    /**
     * Check if user has permission to access resource
     */
    protected boolean canAccessResource(Long resourceUserId, Long resourceTenantId) {
        return SecurityUtils.canAccessResource(resourceUserId, resourceTenantId);
    }

    /**
     * Verify tenant access
     */
    protected void verifyTenantAccess(Long tenantId) {
        if (!canAccessTenant(tenantId)) {
            throw new SecurityException("Access denied to tenant: " + tenantId);
        }
    }

    /**
     * Verify resource access
     */
    protected void verifyResourceAccess(Long resourceUserId, Long resourceTenantId) {
        if (!canAccessResource(resourceUserId, resourceTenantId)) {
            throw new SecurityException("Access denied to resource");
        }
    }

    /**
     * API Response wrapper class
     */
    @lombok.Data
    protected static class ApiResponse<T> {
        private final boolean success;
        private final T data;
        private final String message;
        private final long timestamp;

        public ApiResponse(boolean success, T data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Pagination response wrapper class
     */
    @lombok.Data
    protected static class PageResponse<T> {
        private final List<T> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        public PageResponse(Page<T> page) {
            this.content = page.getContent();
            this.page = page.getNumber();
            this.size = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
        }
    }

    /**
     * Sort parameters helper class
     */
    @lombok.Data
    protected static class SortParams {
        private final String field;
        private final String direction;

        public Sort.Direction getSortDirection() {
            return Optional.ofNullable(direction)
                .map(String::toUpperCase)
                .map(Sort.Direction::valueOf)
                .orElse(Sort.Direction.ASC);
        }

        public Sort getSort() {
            return Sort.by(getSortDirection(), field);
        }
    }
}
