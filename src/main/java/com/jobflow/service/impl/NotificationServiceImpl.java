package com.jobflow.service.impl;

import com.jobflow.dao.NotificationDao;
import com.jobflow.dao.OperationLogDao;
import com.jobflow.domain.*;
import com.jobflow.service.AbstractBaseService;
import com.jobflow.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl extends AbstractBaseService<Notification> implements NotificationService {

    private final NotificationDao notificationDao;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    @Value("${notification.email.from}")
    private String emailFrom;

    @Value("${notification.wechat.api-url}")
    private String wechatApiUrl;

    @Value("${notification.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${notification.retry.interval}")
    private long retryInterval;

    @Autowired
    public NotificationServiceImpl(NotificationDao notificationDao,
                                 OperationLogDao operationLogDao,
                                 JavaMailSender mailSender) {
        super(notificationDao, operationLogDao);
        this.notificationDao = notificationDao;
        this.mailSender = mailSender;
        this.restTemplate = new RestTemplate();
        this.executorService = Executors.newFixedThreadPool(5);
    }

    @Override
    protected OperationLog.OperationModule getOperationModule() {
        return OperationLog.OperationModule.NOTIFICATION;
    }

    @Override
    protected String getEntityName() {
        return "Notification";
    }

    @Override
    @Transactional
    public boolean sendNotification(Notification notification, String operator) {
        notification.setStatus(Notification.NotificationStatus.PENDING);
        Long id = notificationDao.insert(notification, operator);
        notification.setId(id);

        executorService.submit(() -> sendNotificationAsync(notification, operator));
        return true;
    }

    private void sendNotificationAsync(Notification notification, String operator) {
        try {
            boolean success = false;
            String response = null;

            switch (notification.getType()) {
                case EMAIL:
                    success = sendEmail(notification);
                    break;
                case WECHAT:
                    success = sendWechatMessage(notification);
                    break;
                case WEBHOOK:
                    success = sendWebhook(notification);
                    break;
            }

            if (success) {
                notification.markAsSent(response);
            } else {
                notification.markAsFailed("Failed to send notification");
                if (notification.isRetryable()) {
                    notification.incrementRetries();
                }
            }

            notificationDao.update(notification, operator);

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notification.markAsFailed(e.getMessage());
            notificationDao.update(notification, operator);
        }
    }

    private boolean sendEmail(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(emailFrom);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getTitle());
            helper.setText(notification.getContent(), true);
            
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendWechatMessage(Notification notification) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("touser", notification.getRecipient());
            request.put("msgtype", "text");
            request.put("text", Map.of("content", notification.getContent()));

            restTemplate.postForObject(wechatApiUrl, request, Map.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to send WeChat message: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendWebhook(Notification notification) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", notification.getTitle());
            request.put("content", notification.getContent());
            request.put("level", notification.getLevel());
            request.put("timestamp", System.currentTimeMillis());

            restTemplate.postForObject(notification.getRecipient(), request, Map.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to send webhook: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendTaskNotification(Task task, User user,
                                      Notification.NotificationType type,
                                      Notification.NotificationLevel level,
                                      String content, String operator) {
        Notification notification = Notification.createTaskNotification(
            task, user, type, level, content);
        return sendNotification(notification, operator);
    }

    @Override
    public boolean sendWorkflowNotification(Workflow workflow, User user,
                                          Notification.NotificationType type,
                                          Notification.NotificationLevel level,
                                          String content, String operator) {
        Notification notification = Notification.createWorkflowNotification(
            workflow, user, type, level, content);
        return sendNotification(notification, operator);
    }

    @Override
    public int sendSystemNotification(List<User> users,
                                    Notification.NotificationType type,
                                    Notification.NotificationLevel level,
                                    String title, String content, String operator) {
        int successCount = 0;
        for (User user : users) {
            if (!user.canReceiveNotifications()) {
                continue;
            }

            Notification notification = new Notification();
            notification.setType(type);
            notification.setLevel(level);
            notification.setSource(Notification.NotificationSource.SYSTEM);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setUserId(user.getId());
            notification.setTenantId(user.getTenantId());

            switch (type) {
                case EMAIL:
                    notification.setRecipient(user.getEmail());
                    break;
                case WECHAT:
                    notification.setRecipient(user.getWechatId());
                    break;
                case WEBHOOK:
                    notification.setRecipient(user.getPreferences()); // Assuming webhook URL
                    break;
            }

            if (sendNotification(notification, operator)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    @Transactional
    public boolean retryNotification(Long notificationId, String operator) {
        Notification notification = findById(notificationId);
        if (notification == null || !notification.isRetryable()) {
            return false;
        }

        notification.setStatus(Notification.NotificationStatus.PENDING);
        notificationDao.update(notification, operator);
        
        executorService.submit(() -> sendNotificationAsync(notification, operator));
        return true;
    }

    @Override
    @Transactional
    public void cancelNotification(Long notificationId, String operator) {
        Notification notification = findById(notificationId);
        if (notification == null || 
            notification.getStatus() != Notification.NotificationStatus.PENDING) {
            return;
        }

        notification.setStatus(Notification.NotificationStatus.CANCELLED);
        notificationDao.update(notification, operator);
    }

    @Override
    public List<Notification> findPendingNotifications() {
        return notificationDao.findPendingNotifications();
    }

    @Override
    public List<Notification> findRetryableNotifications() {
        return notificationDao.findRetryableNotifications();
    }

    @Override
    public List<Notification> findBySource(Notification.NotificationSource source,
                                         Long sourceId, Long tenantId) {
        return notificationDao.findBySource(source, sourceId, tenantId);
    }

    @Override
    public List<Notification> findByUser(Long userId, Long tenantId) {
        return notificationDao.findByUser(userId, tenantId);
    }

    @Override
    public NotificationStatistics getNotificationStatistics(Long tenantId) {
        List<Notification> notifications = findByTenantId(tenantId);
        NotificationStatistics stats = new NotificationStatistics();
        
        stats.setTotalNotifications(notifications.size());
        stats.setPendingNotifications(notifications.stream()
            .filter(n -> n.getStatus() == Notification.NotificationStatus.PENDING)
            .count());
        stats.setSentNotifications(notifications.stream()
            .filter(n -> n.getStatus() == Notification.NotificationStatus.SENT)
            .count());
        stats.setFailedNotifications(notifications.stream()
            .filter(n -> n.getStatus() == Notification.NotificationStatus.FAILED)
            .count());

        // Group by type
        stats.setNotificationsByType(notifications.stream()
            .collect(Collectors.groupingBy(Notification::getType, Collectors.counting())));

        // Group by level
        stats.setNotificationsByLevel(notifications.stream()
            .collect(Collectors.groupingBy(Notification::getLevel, Collectors.counting())));

        // Group by source
        stats.setNotificationsBySource(notifications.stream()
            .collect(Collectors.groupingBy(Notification::getSource, Collectors.counting())));

        // Calculate average delivery time
        double avgDeliveryTime = notifications.stream()
            .filter(n -> n.getSentTime() != null)
            .mapToLong(n -> java.time.Duration.between(
                n.getScheduledTime(), n.getSentTime()).toMillis())
            .average()
            .orElse(0.0);
        stats.setAverageDeliveryTime(avgDeliveryTime);

        // Calculate success rate
        stats.setSuccessRate(stats.getSentNotifications() * 100.0 / stats.getTotalNotifications());

        return stats;
    }

    @Override
    public boolean testNotificationChannel(Notification.NotificationType type,
                                         String recipient, String operator) {
        Notification testNotification = new Notification();
        testNotification.setType(type);
        testNotification.setLevel(Notification.NotificationLevel.INFO);
        testNotification.setSource(Notification.NotificationSource.SYSTEM);
        testNotification.setTitle("Test Notification");
        testNotification.setContent("This is a test notification.");
        testNotification.setRecipient(recipient);
        testNotification.setScheduledTime(LocalDateTime.now());

        try {
            boolean success = false;
            switch (type) {
                case EMAIL:
                    success = sendEmail(testNotification);
                    break;
                case WECHAT:
                    success = sendWechatMessage(testNotification);
                    break;
                case WEBHOOK:
                    success = sendWebhook(testNotification);
                    break;
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to test notification channel: {}", e.getMessage());
            return false;
        }
    }
}
