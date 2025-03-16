package com.jobflow.service.impl;

import com.jobflow.dao.TaskDao;
import com.jobflow.dao.OperationLogDao;
import com.jobflow.dao.NotificationDao;
import com.jobflow.domain.Task;
import com.jobflow.domain.OperationLog;
import com.jobflow.domain.Notification;
import com.jobflow.lock.DistributedLock;
import com.jobflow.service.AbstractBaseService;
import com.jobflow.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class TaskServiceImpl extends AbstractBaseService<Task> implements TaskService {

    private final TaskDao taskDao;
    private final NotificationDao notificationDao;
    private final DistributedLock distributedLock;
    private final ExecutorService executorService;

    @Autowired
    public TaskServiceImpl(TaskDao taskDao, 
                         OperationLogDao operationLogDao,
                         NotificationDao notificationDao,
                         DistributedLock distributedLock) {
        super(taskDao, operationLogDao);
        this.taskDao = taskDao;
        this.notificationDao = notificationDao;
        this.distributedLock = distributedLock;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    protected OperationLog.OperationModule getOperationModule() {
        return OperationLog.OperationModule.TASK;
    }

    @Override
    protected String getEntityName() {
        return "Task";
    }

    @Override
    public List<Task> findByGroupName(String groupName, Long tenantId) {
        return taskDao.findByGroupName(groupName, tenantId);
    }

    @Override
    public List<Task> findDueTasks(Long tenantId) {
        return taskDao.findDueTasks(tenantId);
    }

    @Override
    @Transactional
    public void executeTask(Task task, String operator) {
        String lockKey = "task_execution_" + task.getId();
        
        try {
            if (!distributedLock.acquire(lockKey)) {
                log.warn("Failed to acquire lock for task: {}", task.getId());
                return;
            }

            if (!canExecute(task)) {
                log.warn("Task {} is not executable", task.getId());
                return;
            }

            task.markAsRunning();
            taskDao.update(task, operator);

            Future<?> future = executorService.submit(() -> executeTaskInternal(task));

            try {
                future.get(task.getTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                markAsTimeout(task.getId(), operator);
            } catch (Exception e) {
                log.error("Task execution failed: {}", e.getMessage());
                markAsCompleted(task.getId(), false, e.getMessage(), operator);
            }

        } finally {
            distributedLock.release(lockKey);
        }
    }

    private void executeTaskInternal(Task task) {
        // Implementation depends on task type (HTTP, SHELL, SPRING_BEAN)
        switch (task.getType()) {
            case HTTP:
                executeHttpTask(task);
                break;
            case SHELL:
                executeShellTask(task);
                break;
            case SPRING_BEAN:
                executeSpringBeanTask(task);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported task type: " + task.getType());
        }
    }

    private void executeHttpTask(Task task) {
        // TODO: Implement HTTP task execution
    }

    private void executeShellTask(Task task) {
        // TODO: Implement Shell task execution
    }

    private void executeSpringBeanTask(Task task) {
        // TODO: Implement Spring Bean task execution
    }

    @Override
    @Transactional
    public void retryTask(Long taskId, String operator) {
        Task task = findById(taskId);
        if (task == null || !task.isRetryable()) {
            throw new IllegalStateException("Task is not retryable");
        }

        task.incrementRetries();
        taskDao.update(task, operator);
        executeTask(task, operator);
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId, String operator) {
        Task task = findById(taskId);
        if (task == null || task.getStatus() != Task.TaskStatus.RUNNING) {
            throw new IllegalStateException("Task is not running");
        }

        task.setStatus(Task.TaskStatus.CANCELLED);
        taskDao.update(task, operator);
    }

    @Override
    @Transactional
    public void updateStatus(Long taskId, Task.TaskStatus status, String operator) {
        taskDao.updateStatus(taskId, status, operator);
    }

    @Override
    public Task findByName(String name, Long tenantId) {
        return taskDao.findByName(name, tenantId);
    }

    @Override
    public boolean canExecute(Task task) {
        if (task == null || !task.isExecutable()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return (task.getStartTime() == null || !now.isBefore(task.getStartTime())) &&
               (task.getEndTime() == null || !now.isAfter(task.getEndTime()));
    }

    @Override
    public void calculateNextExecutionTime(Task task) {
        if (task.getCronExpression() == null) {
            return;
        }

        try {
            CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
            LocalDateTime next = cronExpression.next(LocalDateTime.now());
            task.setNextExecutionTime(next);
        } catch (Exception e) {
            log.error("Failed to calculate next execution time: {}", e.getMessage());
        }
    }

    @Override
    public boolean isWithinTimeout(Task task) {
        if (task.getLastExecutionTime() == null || task.getTimeout() == null) {
            return true;
        }

        LocalDateTime timeoutTime = task.getLastExecutionTime().plusSeconds(task.getTimeout() / 1000);
        return !LocalDateTime.now().isAfter(timeoutTime);
    }

    @Override
    @Transactional
    public void markAsCompleted(Long taskId, boolean success, String result, String operator) {
        Task task = findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found");
        }

        long duration = System.currentTimeMillis() - 
                       task.getLastExecutionTime().atZone(java.time.ZoneId.systemDefault())
                           .toInstant().toEpochMilli();
        
        task.markAsCompleted(success, result, duration);
        calculateNextExecutionTime(task);
        taskDao.update(task, operator);

        // Send notifications if configured
        if ((success && task.getNotifyOnSuccess()) || (!success && task.getNotifyOnFailure())) {
            sendNotifications(task, success);
        }
    }

    @Override
    @Transactional
    public void markAsTimeout(Long taskId, String operator) {
        Task task = findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found");
        }

        task.markAsTimeout();
        calculateNextExecutionTime(task);
        taskDao.update(task, operator);

        if (task.getNotifyOnFailure()) {
            sendNotifications(task, false);
        }
    }

    private void sendNotifications(Task task, boolean success) {
        // TODO: Implement notification sending logic
    }

    @Override
    public List<Task> getExecutionHistory(Long taskId, Long tenantId) {
        // TODO: Implement execution history retrieval
        return null;
    }

    @Override
    public TaskStatistics getTaskStatistics(Long tenantId) {
        // TODO: Implement task statistics calculation
        return new TaskStatistics();
    }
}
