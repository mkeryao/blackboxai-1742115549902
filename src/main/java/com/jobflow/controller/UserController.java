package com.jobflow.controller;

import com.jobflow.domain.User;
import com.jobflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * User Controller
 * 
 * Handles user management operations including CRUD and role management.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController extends BaseController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a new user")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> createUser(
            @Valid @RequestBody User user) {
        try {
            user.setTenantId(getCurrentTenantId());
            User createdUser = userService.create(user, getCurrentUser().getUsername());
            return success(createdUser, "User created successfully");
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return error("Failed to create user: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update an existing user")
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody User user) {
        try {
            verifyResourceAccess(user.getId(), user.getTenantId());
            user.setId(userId);
            User updatedUser = userService.update(user, getCurrentUser().getUsername());
            return success(updatedUser, "User updated successfully");
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return error("Failed to update user: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            userService.delete(userId, getCurrentUser().getUsername());
            return success(null, "User deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return error("Failed to delete user: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<User>> getUser(
            @PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            return success(user);
        } catch (Exception e) {
            log.error("Failed to get user", e);
            return error("Failed to get user: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get users by role")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRole(
            @PathVariable User.UserRole role) {
        try {
            List<User> users = userService.findByRole(role, getCurrentTenantId());
            return success(users);
        } catch (Exception e) {
            log.error("Failed to get users by role", e);
            return error("Failed to get users by role: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update user roles")
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRoles(
            @PathVariable Long userId,
            @RequestBody Set<User.UserRole> roles) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            userService.updateRoles(userId, roles, getCurrentUser().getUsername());
            return success(null, "User roles updated successfully");
        } catch (Exception e) {
            log.error("Failed to update user roles", e);
            return error("Failed to update user roles: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Change user password")
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            boolean success = userService.changePassword(userId, request.getOldPassword(),
                                                       request.getNewPassword(),
                                                       getCurrentUser().getUsername());
            if (success) {
                return success(null, "Password changed successfully");
            } else {
                return error("Invalid old password", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("Failed to change password", e);
            return error("Failed to change password: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Reset user password")
    @PostMapping("/{userId}/password/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            String newPassword = userService.resetPassword(userId, getCurrentUser().getUsername());
            return success(newPassword, "Password reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset password", e);
            return error("Failed to reset password: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Lock user account")
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            userService.lockUser(userId, getCurrentUser().getUsername());
            return success(null, "User account locked successfully");
        } catch (Exception e) {
            log.error("Failed to lock user account", e);
            return error("Failed to lock user account: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Unlock user account")
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            verifyResourceAccess(user.getId(), user.getTenantId());
            userService.unlockUser(userId, getCurrentUser().getUsername());
            return success(null, "User account unlocked successfully");
        } catch (Exception e) {
            log.error("Failed to unlock user account", e);
            return error("Failed to unlock user account: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get user statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserService.UserStatistics>> getUserStatistics() {
        try {
            UserService.UserStatistics statistics = userService.getUserStatistics(getCurrentTenantId());
            return success(statistics);
        } catch (Exception e) {
            log.error("Failed to get user statistics", e);
            return error("Failed to get user statistics: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @lombok.Data
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;
    }
}
