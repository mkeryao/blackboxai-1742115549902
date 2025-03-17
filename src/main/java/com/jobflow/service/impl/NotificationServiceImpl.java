package com.jobflow.service.impl;

import com.jobflow.dao.NotificationDao;
import com.jobflow.domain.NotificationConfig;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import com.jobflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;
    private final JavaMailSender mailSender;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    private final Map<NotificationConfig.NotificationChannel, Integer> rateLimits = new ConcurrentHashMap<>();
    private final Map<String, Integer> notificationCounts = new ConcurrentHashMap<>();

    @Override
    public void sendTaskNotifications(Task task, NotificationConfig.NotificationType eventType, Map<String, Object> context) {
        if (!notificationsEnabled) {
            log.info("Notifications are disabled");
            return;
        }

        List<NotificationConfig> configs = task.getNotificationsForEvent(eventType);
        for (NotificationConfig config : configs) {
            try {
                String content = processTemplate(config.getTemplate(), enrichTaskContext(context, task));
                sendNotification(config, content, context);
            } catch (Exception e) {
                log.error("Failed to send task notification", e);
                saveFailedNotification("TASK", task.getId(), eventType, config, e.getMessage());
            }
        }
    }

    @Override
    public void sendWorkflowNotifications(Workflow workflow, NotificationConfig.NotificationType eventType, Map<String, Object> context) {
        if (!notificationsEnabled) {
            log.info("Notifications are disabled");
            return;
        }

        List<NotificationConfig> configs = workflow.getNotificationsForEvent(eventType);
        for (NotificationConfig config : configs) {
            try {
                String content = processTemplate(config.getTemplate(), enrichWorkflowContext(context, workflow));
                sendNotification(config, content, context);
            } catch (Exception e) {
                log.error("Failed to send workflow notification", e);
                saveFailedNotification("WORKFLOW", workflow.getId(), eventType, config, e.getMessage());
            }
        }
    }

    @Override
    public void sendNotification(NotificationConfig config, String content, Map<String, Object> context) {
        if (!checkRateLimit(config.getChannels().get(0))) {
            log.warn("Rate limit exceeded for channel: {}", config.getChannels().get(0));
            return;
        }

        for (NotificationConfig.NotificationChannel channel : config.getChannels()) {
            try {
                switch (channel) {
                    case EMAIL:
                        sendEmailNotification(config.getRecipients(), content);
                        break;
                    case SLACK:
                        sendSlackNotification(config.getRecipients(), content);
                        break;
                    case WEBHOOK:
                        sendWebhookNotification(config.getRecipients(), content);
                        break;
                    case SMS:
                        sendSmsNotification(config.getRecipients(), content);
                        break;
                    case SYSTEM:
                        sendSystemNotification(config.getRecipients(), content);
                        break;
                }
                incrementNotificationCount(channel.toString());
            } catch (Exception e) {
                log.error("Failed to send notification via channel: {}", channel, e);
            }
        }
    }

    private void sendEmailNotification(List<String> recipients, String content) {
        // Implementation for email notifications
    }

    private void sendSlackNotification(List<String> recipients, String content) {
        // Implementation for Slack notifications
    }

    private void sendWebhookNotification(List<String> recipients, String content) {
        // Implementation for webhook notifications
    }

    private void sendSmsNotification(List<String> recipients, String content) {
        // Implementation for SMS notifications
    }

    private void sendSystemNotification(List<String> recipients, String content) {
        // Implementation for system notifications
    }

    @Override
    public String processTemplate(String template, Map<String, Object> context) {
        if (template == null) {
            return "";
        }

        String result = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", 
                                 entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    private Map<String, Object> enrichTaskContext(Map<String, Object> context, Task task) {
        Map<String, Object> enriched = new HashMap<>(context);
        enriched.put("taskName", task.getName());
        enriched.put("taskId", task.getId());
        enriched.put("taskStatus", task.getStatus());
        return enriched;
    }

    private Map<String, Object> enrichWorkflowContext(Map<String, Object> context, Workflow workflow) {
        Map<String, Object> enriched = new HashMap<>(context);
        enriched.put("workflowName", workflow.getName());
        enriched.put("workflowId", workflow.getId());
        enriched.put("workflowStatus", workflow.getStatus());
        return enriched;
    }

    private boolean checkRateLimit(NotificationConfig.NotificationChannel channel) {
        Integer limit = rateLimits.get(channel);
        if (limit == null) {
            return true;
        }

        String key = channel.toString() + "_" + LocalDateTime.now().getHour();
        Integer count = notificationCounts.getOrDefault(key, 0);
        return count < limit;
    }

    private void incrementNotificationCount(String channel) {
        String key = channel + "_" + LocalDateTime.now().getHour();
        notificationCounts.merge(key, 1, Integer::sum);
    }

    private void saveFailedNotification(String type, Long sourceId, NotificationConfig.NotificationType eventType,
                                      NotificationConfig config, String errorMessage) {
        notificationDao.saveFailedNotification(type, sourceId, eventType, config, errorMessage);
    }

    @Override
    public List<Map<String, Object>> getTaskNotificationHistory(Long taskId) {
        return notificationDao.getNotificationHistory("TASK", taskId);
    }

    @Override
    public List<Map<String, Object>> getWorkflowNotificationHistory(Long workflowId) {
        return notificationDao.getNotificationHistory("WORKFLOW", workflowId);
    }

    @Override
    public Map<String, Object> getNotificationStatistics(Long tenantId) {
        return notificationDao.getNotificationStatistics(tenantId);
    }

    @Override
    public boolean testNotificationConfig(NotificationConfig config) {
        try {
            String testContent = "Test notification from JobFlow";
            sendNotification(config, testContent, new HashMap<>());
            return true;
        } catch (Exception e) {
            log.error("Failed to test notification config", e);
            return false;
        }
    }

    @Override
    public List<NotificationConfig.NotificationChannel> getSupportedChannels() {
        return Arrays.asList(NotificationConfig.NotificationChannel.values());
    }

    @Override
    public Map<String, String> getNotificationTemplates() {
        return Map.of(
            "SUCCESS", NotificationConfig.DefaultTemplates.SUCCESS,
            "FAILURE", NotificationConfig.DefaultTemplates.FAILURE,
            "RETRY", NotificationConfig.DefaultTemplates.RETRY,
            "TIMEOUT", NotificationConfig.DefaultTemplates.TIMEOUT
        );
    }

    @Override
    public boolean validateTemplate(String template) {
        try {
            Map<String, Object> testContext = new HashMap<>();
            testContext.put("test", "value");
            processTemplate(template, testContext);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getNotificationRecipients(NotificationConfig config) {
        return config.getRecipients();
    }

    @Override
    public boolean validateRecipient(String recipient, NotificationConfig.NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return recipient.matches("^[A-Za-z0-9+_.-]+@(.+)$");
            case SLACK:
                return recipient.startsWith("#") || recipient.startsWith("@");
            case WEBHOOK:
                return recipient.startsWith("http://") || recipient.startsWith("https://");
            case SMS:
                return recipient.matches("^\\+?[1-9]\\d{1,14}$");
            case SYSTEM:
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFailedNotifications() {
        return notificationDao.getFailedNotifications();
    }

    @Override
    public boolean retryFailedNotification(Long notificationId) {
        return notificationDao.retryFailedNotification(notificationId);
    }

    @Override
    public Map<String, Object> getNotificationSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", notificationsEnabled);
        settings.put("rateLimits", rateLimits);
        return settings;
    }

    @Override
    public void updateNotificationSettings(Map<String, Object> settings) {
        if (settings.containsKey("enabled")) {
            notificationsEnabled = (Boolean) settings.get("enabled");
        }
        if (settings.containsKey("rateLimits")) {
            @SuppressWarnings("unchecked")
            Map<NotificationConfig.NotificationChannel, Integer> newLimits = 
                (Map<NotificationConfig.NotificationChannel, Integer>) settings.get("rateLimits");
            rateLimits.clear();
            rateLimits.putAll(newLimits);
        }
    }

    @Override
    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    @Override
    public boolean areNotificationsEnabled() {
        return notificationsEnabled;
    }

    @Override
    public Map<String, Integer> getNotificationRateLimits() {
        Map<String, Integer> limits = new HashMap<>();
        rateLimits.forEach((k, v) -> limits.put(k.toString(), v));
        return limits;
    }

    @Override
    public void updateNotificationRateLimits(Map<String, Integer> rateLimits) {
        this.rateLimits.clear();
        rateLimits.forEach((k, v) -> this.rateLimits.put(NotificationConfig.NotificationChannel.valueOf(k), v));
    }
}
