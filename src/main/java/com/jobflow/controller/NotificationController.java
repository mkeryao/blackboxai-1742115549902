package com.jobflow.controller;

import com.jobflow.domain.Notification;
import com.jobflow.domain.User;
import com.jobflow.service.NotificationService;
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

/**
 * Notification Controller
 * 
 * Handles notification-related operations including sending and managing notifications.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "APIs for managing notifications")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Operation(summary = "Send a notification")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Boolean>> sendNotification(
            @Valid @RequestBody Notification notification) {
        try {
            notification.setTenantId(getCurrentTenantId());
            boolean sent = notificationService.sendNotification(notification, getCurrentUser().getUsername());
            return success(sent, sent ? "Notification sent successfully" : "Failed to send notification");
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return error("Failed to send notification: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Send system notification to multiple users")
    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> sendSystemNotification(
            @RequestBody SystemNotificationRequest request) {
        try {
            List<User> users = userService.findByTenantId(getCurrentTenantId());
            int sentCount = notificationService.sendSystemNotification(
                users,
                request.getType(),
                request.getLevel(),
                request.getTitle(),
                request.getContent(),
                getCurrentUser().getUsername()
            );
            return success(sentCount, "System notification sent to " + sentCount + " users");
        } catch (Exception e) {
            log.error("Failed to send system notification", e);
            return error("Failed to send system notification: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Retry a failed notification")
    @PostMapping("/{notificationId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Boolean>> retryNotification(
            @PathVariable Long notificationId) {
        try {
            Notification notification = notificationService.findById(notificationId);
            verifyResourceAccess(notification.getCreatedBy(), notification.getTenantId());
            boolean retried = notificationService.retryNotification(notificationId, getCurrentUser().getUsername());
            return success(retried, retried ? "Notification retry successful" : "Failed to retry notification");
        } catch (Exception e) {
            log.error("Failed to retry notification", e);
            return error("Failed to retry notification: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Cancel a pending notification")
    @PostMapping("/{notificationId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> cancelNotification(
            @PathVariable Long notificationId) {
        try {
            Notification notification = notificationService.findById(notificationId);
            verifyResourceAccess(notification.getCreatedBy(), notification.getTenantId());
            notificationService.cancelNotification(notificationId, getCurrentUser().getUsername());
            return success(null, "Notification cancelled successfully");
        } catch (Exception e) {
            log.error("Failed to cancel notification", e);
            return error("Failed to cancel notification: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get pending notifications")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Notification>>> getPendingNotifications() {
        try {
            List<Notification> notifications = notificationService.findPendingNotifications();
            return success(notifications);
        } catch (Exception e) {
            log.error("Failed to get pending notifications", e);
            return error("Failed to get pending notifications: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get notifications by source")
    @GetMapping("/source/{source}/{sourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotificationsBySource(
            @PathVariable Notification.NotificationSource source,
            @PathVariable Long sourceId) {
        try {
            List<Notification> notifications = notificationService.findBySource(
                source, sourceId, getCurrentTenantId());
            return success(notifications);
        } catch (Exception e) {
            log.error("Failed to get notifications by source", e);
            return error("Failed to get notifications by source: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get user notifications")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<List<Notification>>> getUserNotifications(
            @PathVariable Long userId) {
        try {
            List<Notification> notifications = notificationService.findByUser(userId, getCurrentTenantId());
            return success(notifications);
        } catch (Exception e) {
            log.error("Failed to get user notifications", e);
            return error("Failed to get user notifications: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get notification statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<NotificationService.NotificationStatistics>> getNotificationStatistics() {
        try {
            NotificationService.NotificationStatistics statistics = 
                notificationService.getNotificationStatistics(getCurrentTenantId());
            return success(statistics);
        } catch (Exception e) {
            log.error("Failed to get notification statistics", e);
            return error("Failed to get notification statistics: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Test notification channel")
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> testNotificationChannel(
            @RequestBody NotificationChannelTestRequest request) {
        try {
            boolean success = notificationService.testNotificationChannel(
                request.getType(),
                request.getRecipient(),
                getCurrentUser().getUsername()
            );
            return success(success, success ? "Test notification sent successfully" : 
                                           "Failed to send test notification");
        } catch (Exception e) {
            log.error("Failed to test notification channel", e);
            return error("Failed to test notification channel: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @lombok.Data
    public static class SystemNotificationRequest {
        private Notification.NotificationType type;
        private Notification.NotificationLevel level;
        private String title;
        private String content;
    }

    @lombok.Data
    public static class NotificationChannelTestRequest {
        private Notification.NotificationType type;
        private String recipient;
    }
}
