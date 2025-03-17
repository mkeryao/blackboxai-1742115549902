package com.jobflow.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for security operations
 */
public class SecurityUtils {
    
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";

    /**
     * Get current user ID from request
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(USER_ID_ATTRIBUTE);
    }

    /**
     * Get current tenant ID from request
     */
    public static Long getCurrentTenantId(HttpServletRequest request) {
        return (Long) request.getAttribute(TENANT_ID_ATTRIBUTE);
    }

    /**
     * Check if user has access to tenant
     */
    public static boolean hasAccessToTenant(HttpServletRequest request, Long tenantId) {
        Long currentTenantId = getCurrentTenantId(request);
        return currentTenantId != null && currentTenantId.equals(tenantId);
    }

    /**
     * Simple password hashing (should use proper password hashing in production)
     */
    public static String hashPassword(String password) {
        // TODO: Implement proper password hashing (e.g., BCrypt)
        return password;
    }

    /**
     * Verify password (should use proper password verification in production)
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        // TODO: Implement proper password verification
        return rawPassword.equals(hashedPassword);
    }
}
