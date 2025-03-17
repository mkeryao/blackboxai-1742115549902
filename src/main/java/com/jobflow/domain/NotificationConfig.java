package com.jobflow.domain;

import lombok.Data;

import java.util.List;

@Data
public class NotificationConfig {
    private List<NotificationType> notifyOn;  // When to send notifications
    private List<NotificationChannel> channels;  // How to send notifications
    private List<String> recipients;  // Who to notify
    private String template;  // Notification template
    private boolean enabled = true;  // Whether notifications are enabled

    public enum NotificationType {
        SUCCESS,      // Notify on successful completion
        FAILURE,      // Notify on failure
        START,        // Notify when starting
        RETRY,        // Notify on retry attempts
        TIMEOUT,      // Notify on timeout
        CANCELLED,    // Notify when cancelled
        WARNING       // Notify on warnings
    }

    public enum NotificationChannel {
        EMAIL,        // Send email notifications
        SLACK,        // Send Slack notifications
        WEBHOOK,      // Send webhook notifications
        SMS,          // Send SMS notifications
        SYSTEM        // Send in-system notifications
    }

    // Helper methods to check notification conditions
    public boolean shouldNotifyOn(NotificationType type) {
        return enabled && notifyOn != null && notifyOn.contains(type);
    }

    public boolean hasChannel(NotificationChannel channel) {
        return channels != null && channels.contains(channel);
    }

    public boolean hasRecipients() {
        return recipients != null && !recipients.isEmpty();
    }

    // Template variables for notifications
    public static class TemplateVariables {
        public static final String TASK_NAME = "${taskName}";
        public static final String WORKFLOW_NAME = "${workflowName}";
        public static final String STATUS = "${status}";
        public static final String START_TIME = "${startTime}";
        public static final String END_TIME = "${endTime}";
        public static final String DURATION = "${duration}";
        public static final String ERROR_MESSAGE = "${errorMessage}";
        public static final String EXECUTOR = "${executor}";
        public static final String RETRY_COUNT = "${retryCount}";
        public static final String NEXT_RETRY_TIME = "${nextRetryTime}";
    }

    // Default templates for different notification types
    public static class DefaultTemplates {
        public static final String SUCCESS = """
            ${taskName} completed successfully
            Start Time: ${startTime}
            End Time: ${endTime}
            Duration: ${duration}
            Executor: ${executor}
            """;

        public static final String FAILURE = """
            ${taskName} failed
            Error: ${errorMessage}
            Start Time: ${startTime}
            End Time: ${endTime}
            Executor: ${executor}
            """;

        public static final String RETRY = """
            ${taskName} will be retried
            Error: ${errorMessage}
            Retry Count: ${retryCount}
            Next Retry: ${nextRetryTime}
            """;

        public static final String TIMEOUT = """
            ${taskName} timed out
            Start Time: ${startTime}
            Duration: ${duration}
            Executor: ${executor}
            """;
    }
}
