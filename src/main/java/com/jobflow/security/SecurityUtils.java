package com.jobflow.security;

import com.jobflow.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security Utilities
 * 
 * Provides utility methods for security-related operations.
 */
@Component
public class SecurityUtils {

    /**
     * Get the current authenticated user
     */
    public static Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return Optional.of((User) principal);
        }
        
        return Optional.empty();
    }

    /**
     * Get the current user ID
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /**
     * Get the current tenant ID
     */
    public static Optional<Long> getCurrentTenantId() {
        return getCurrentUser().map(User::getTenantId);
    }

    /**
     * Check if the current user has a specific role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    /**
     * Check if the current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        Set<String> userRoles = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());
            
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if the current user has all specified roles
     */
    public static boolean hasAllRoles(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        Set<String> userRoles = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());
            
        for (String role : roles) {
            if (!userRoles.contains(role)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Check if the current user is an administrator
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Check if the current user owns the specified resource
     */
    public static boolean isResourceOwner(Long resourceUserId) {
        return getCurrentUserId()
            .map(userId -> userId.equals(resourceUserId))
            .orElse(false);
    }

    /**
     * Check if the current user belongs to the specified tenant
     */
    public static boolean belongsToTenant(Long tenantId) {
        return getCurrentTenantId()
            .map(currentTenantId -> currentTenantId.equals(tenantId))
            .orElse(false);
    }

    /**
     * Get the current user's roles
     */
    public static Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        
        return authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());
    }

    /**
     * Check if the current context has authentication
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Clear the security context
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Get the current authentication token
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Check if the current user can access the specified tenant
     */
    public static boolean canAccessTenant(Long tenantId) {
        return isAdmin() || belongsToTenant(tenantId);
    }

    /**
     * Check if the current user can access the specified resource
     */
    public static boolean canAccessResource(Long resourceUserId, Long resourceTenantId) {
        return isAdmin() || 
               isResourceOwner(resourceUserId) || 
               belongsToTenant(resourceTenantId);
    }
}
