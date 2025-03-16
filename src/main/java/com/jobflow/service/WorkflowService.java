package com.jobflow.service;

import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import com.jobflow.domain.WorkflowDependency;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing workflows
 */
public interface WorkflowService extends BaseService<Workflow> {
    
    /**
     * Find workflows that are due for execution
     */
    List<Workflow> findDueWorkflows(Long tenantId);

    /**
     * Execute a workflow
     */
    void executeWorkflow(Workflow workflow, String operator);

    /**
     * Execute workflow from a specific task
     */
    void executeWorkflowFromTask(Long workflowId, Long taskId, String operator);

    /**
     * Cancel a running workflow
     */
    void cancelWorkflow(Long workflowId, String operator);

    /**
     * Update workflow status
     */
    void updateStatus(Long workflowId, Workflow.WorkflowStatus status, String operator);

    /**
     * Find workflow by name
     */
    Workflow findByName(String name, Long tenantId);

    /**
     * Add task to workflow
     */
    void addTask(Long workflowId, Task task, String operator);

    /**
     * Remove task from workflow
     */
    void removeTask(Long workflowId, Long taskId, String operator);

    /**
     * Add dependency between tasks
     */
    void addDependency(Long workflowId, Long sourceTaskId, Long targetTaskId, 
                      WorkflowDependency.DependencyType type, String operator);

    /**
     * Remove dependency between tasks
     */
    void removeDependency(Long workflowId, Long sourceTaskId, Long targetTaskId, String operator);

    /**
     * Get all tasks in workflow
     */
    List<Task> getWorkflowTasks(Long workflowId);

    /**
     * Get all dependencies in workflow
     */
    List<WorkflowDependency> getWorkflowDependencies(Long workflowId);

    /**
     * Validate workflow DAG (no cycles)
     */
    boolean validateWorkflowDag(Long workflowId);

    /**
     * Get topological sort of workflow tasks
     */
    List<Task> getTopologicalSort(Long workflowId);

    /**
     * Get workflow execution history
     */
    List<Workflow> getExecutionHistory(Long workflowId, Long tenantId);

    /**
     * Get workflow statistics
     */
    WorkflowStatistics getWorkflowStatistics(Long tenantId);

    /**
     * Get task dependencies (both upstream and downstream)
     */
    Map<String, List<Task>> getTaskDependencies(Long workflowId, Long taskId);

    /**
     * Calculate workflow progress
     */
    double getWorkflowProgress(Long workflowId);

    /**
     * Get estimated completion time based on historical execution times
     */
    java.time.LocalDateTime getEstimatedCompletionTime(Long workflowId);

    /**
     * Inner class for workflow statistics
     */
    class WorkflowStatistics {
        private long totalWorkflows;
        private long activeWorkflows;
        private long successfulWorkflows;
        private long failedWorkflows;
        private long runningWorkflows;
        private double averageExecutionTime;
        private double successRate;
        private int averageTasksPerWorkflow;
        private int maxTasksInWorkflow;

        // Getters and setters
        public long getTotalWorkflows() { return totalWorkflows; }
        public void setTotalWorkflows(long totalWorkflows) { this.totalWorkflows = totalWorkflows; }
        
        public long getActiveWorkflows() { return activeWorkflows; }
        public void setActiveWorkflows(long activeWorkflows) { this.activeWorkflows = activeWorkflows; }
        
        public long getSuccessfulWorkflows() { return successfulWorkflows; }
        public void setSuccessfulWorkflows(long successfulWorkflows) { 
            this.successfulWorkflows = successfulWorkflows; 
        }
        
        public long getFailedWorkflows() { return failedWorkflows; }
        public void setFailedWorkflows(long failedWorkflows) { this.failedWorkflows = failedWorkflows; }
        
        public long getRunningWorkflows() { return runningWorkflows; }
        public void setRunningWorkflows(long runningWorkflows) { this.runningWorkflows = runningWorkflows; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { 
            this.averageExecutionTime = averageExecutionTime; 
        }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public int getAverageTasksPerWorkflow() { return averageTasksPerWorkflow; }
        public void setAverageTasksPerWorkflow(int averageTasksPerWorkflow) { 
            this.averageTasksPerWorkflow = averageTasksPerWorkflow; 
        }
        
        public int getMaxTasksInWorkflow() { return maxTasksInWorkflow; }
        public void setMaxTasksInWorkflow(int maxTasksInWorkflow) { 
            this.maxTasksInWorkflow = maxTasksInWorkflow; 
        }
    }
}
