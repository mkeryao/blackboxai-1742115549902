package com.jobflow.service;

import com.jobflow.domain.Task;
import java.util.List;

/**
 * Service interface for managing tasks
 */
public interface TaskService extends BaseService<Task> {
    
    /**
     * Find tasks by group name
     */
    List<Task> findByGroupName(String groupName, Long tenantId);

    /**
     * Find tasks that are due for execution
     */
    List<Task> findDueTasks(Long tenantId);

    /**
     * Execute a task
     */
    void executeTask(Task task, String operator);

    /**
     * Retry a failed task
     */
    void retryTask(Long taskId, String operator);

    /**
     * Cancel a running task
     */
    void cancelTask(Long taskId, String operator);

    /**
     * Update task status
     */
    void updateStatus(Long taskId, Task.TaskStatus status, String operator);

    /**
     * Find task by name
     */
    Task findByName(String name, Long tenantId);

    /**
     * Check if task can be executed
     */
    boolean canExecute(Task task);

    /**
     * Calculate next execution time based on cron expression
     */
    void calculateNextExecutionTime(Task task);

    /**
     * Check if task is running within timeout
     */
    boolean isWithinTimeout(Task task);

    /**
     * Mark task as completed
     */
    void markAsCompleted(Long taskId, boolean success, String result, String operator);

    /**
     * Mark task as timeout
     */
    void markAsTimeout(Long taskId, String operator);

    /**
     * Get task execution history
     */
    List<Task> getExecutionHistory(Long taskId, Long tenantId);

    /**
     * Get task statistics
     */
    TaskStatistics getTaskStatistics(Long tenantId);

    /**
     * Inner class for task statistics
     */
    class TaskStatistics {
        private long totalTasks;
        private long activeTasks;
        private long successfulTasks;
        private long failedTasks;
        private long runningTasks;
        private double averageExecutionTime;
        private double successRate;

        // Getters and setters
        public long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }
        
        public long getActiveTasks() { return activeTasks; }
        public void setActiveTasks(long activeTasks) { this.activeTasks = activeTasks; }
        
        public long getSuccessfulTasks() { return successfulTasks; }
        public void setSuccessfulTasks(long successfulTasks) { this.successfulTasks = successfulTasks; }
        
        public long getFailedTasks() { return failedTasks; }
        public void setFailedTasks(long failedTasks) { this.failedTasks = failedTasks; }
        
        public long getRunningTasks() { return runningTasks; }
        public void setRunningTasks(long runningTasks) { this.runningTasks = runningTasks; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { 
            this.averageExecutionTime = averageExecutionTime; 
        }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}
