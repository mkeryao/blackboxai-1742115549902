package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowDependency extends BaseEntity {
    
    private Long workflowId;
    private Long sourceTaskId;      // The task that must complete first
    private Long targetTaskId;      // The task that depends on the source task
    
    // Optional condition for the dependency
    private String condition;       // Optional JSON string containing condition logic
    
    // Dependency type (e.g., SUCCESS_REQUIRED, COMPLETION_REQUIRED)
    public enum DependencyType {
        SUCCESS_REQUIRED,           // Target can only run if source succeeded
        COMPLETION_REQUIRED         // Target can run after source completes (success or failure)
    }
    private DependencyType type;
    
    // Timeout for waiting on dependency
    private Long timeout;           // in milliseconds, 0 means wait indefinitely
    
    public WorkflowDependency() {
        this.type = DependencyType.SUCCESS_REQUIRED;
        this.timeout = 0L;
    }
    
    /**
     * Check if this dependency allows the target task to proceed
     * @param sourceTask The source task
     * @return true if the dependency is satisfied
     */
    public boolean isSatisfied(Task sourceTask) {
        if (sourceTask == null) {
            return false;
        }
        
        switch (this.type) {
            case SUCCESS_REQUIRED:
                return sourceTask.getStatus() == Task.TaskStatus.SUCCESS;
                
            case COMPLETION_REQUIRED:
                return sourceTask.getStatus() == Task.TaskStatus.SUCCESS || 
                       sourceTask.getStatus() == Task.TaskStatus.FAILED;
                
            default:
                return false;
        }
    }
    
    /**
     * Check if this dependency has timed out
     * @param startTime The time when we started waiting for this dependency
     * @return true if the dependency has timed out
     */
    public boolean hasTimedOut(long startTime) {
        if (this.timeout == 0L) {
            return false;
        }
        return System.currentTimeMillis() - startTime > this.timeout;
    }
    
    /**
     * Creates a key that uniquely identifies this dependency within a workflow
     * @return String representation of the dependency
     */
    public String getDependencyKey() {
        return String.format("%d_%d_%d", this.workflowId, this.sourceTaskId, this.targetTaskId);
    }
    
    /**
     * Validates that this dependency is properly configured
     * @return true if the dependency is valid
     */
    public boolean isValid() {
        return workflowId != null && 
               sourceTaskId != null && 
               targetTaskId != null && 
               !sourceTaskId.equals(targetTaskId);  // Prevent self-dependency
    }
}
