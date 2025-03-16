package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseEntity {
    
    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE,
        EXECUTE,
        LOGIN,
        LOGOUT,
        SYSTEM
    }

    public enum OperationStatus {
        SUCCESS,
        FAILED
    }

    public enum OperationModule {
        TASK,
        WORKFLOW,
        USER,
        NOTIFICATION,
        SYSTEM,
        SECURITY
    }

    private String operationId;         // Unique operation ID
    private OperationType type;
    private OperationModule module;
    private OperationStatus status;
    
    private Long operatorId;            // User ID who performed the operation
    private String operatorName;        // Username who performed the operation
    
    private String resourceId;          // ID of the resource being operated on
    private String resourceType;        // Type of the resource (e.g., "Task", "Workflow")
    private String resourceName;        // Name of the resource
    
    private String operation;           // Detailed operation description
    private String parameters;          // Operation parameters (JSON)
    private String result;              // Operation result or error message
    
    private String clientIp;            // Client IP address
    private String userAgent;           // User agent information
    private Long duration;              // Operation duration in milliseconds
    
    private LocalDateTime startTime;    // Operation start time
    private LocalDateTime endTime;      // Operation end time

    public OperationLog() {
        this.startTime = LocalDateTime.now();
        this.operationId = generateOperationId();
    }

    /**
     * Generate a unique operation ID
     */
    private String generateOperationId() {
        return String.format("%s_%d", 
            LocalDateTime.now().toString().replace(":", "").replace(".", ""),
            System.nanoTime() % 1000000);
    }

    /**
     * Complete the operation log
     */
    public void complete(OperationStatus status, String result) {
        this.status = status;
        this.result = result;
        this.endTime = LocalDateTime.now();
        this.duration = java.time.Duration.between(startTime, endTime).toMillis();
    }

    /**
     * Mark operation as successful
     */
    public void markAsSuccess(String result) {
        complete(OperationStatus.SUCCESS, result);
    }

    /**
     * Mark operation as failed
     */
    public void markAsFailed(String error) {
        complete(OperationStatus.FAILED, error);
    }

    /**
     * Create a builder for task operations
     */
    public static OperationLogBuilder taskOperation() {
        return new OperationLogBuilder()
            .module(OperationModule.TASK)
            .resourceType("Task");
    }

    /**
     * Create a builder for workflow operations
     */
    public static OperationLogBuilder workflowOperation() {
        return new OperationLogBuilder()
            .module(OperationModule.WORKFLOW)
            .resourceType("Workflow");
    }

    /**
     * Create a builder for user operations
     */
    public static OperationLogBuilder userOperation() {
        return new OperationLogBuilder()
            .module(OperationModule.USER)
            .resourceType("User");
    }

    /**
     * Builder pattern for creating operation logs
     */
    public static class OperationLogBuilder {
        private final OperationLog log;

        public OperationLogBuilder() {
            this.log = new OperationLog();
        }

        public OperationLogBuilder type(OperationType type) {
            log.setType(type);
            return this;
        }

        public OperationLogBuilder module(OperationModule module) {
            log.setModule(module);
            return this;
        }

        public OperationLogBuilder operator(User operator) {
            log.setOperatorId(operator.getId());
            log.setOperatorName(operator.getUsername());
            return this;
        }

        public OperationLogBuilder resource(String id, String name) {
            log.setResourceId(id);
            log.setResourceName(name);
            return this;
        }

        public OperationLogBuilder resourceType(String type) {
            log.setResourceType(type);
            return this;
        }

        public OperationLogBuilder operation(String operation) {
            log.setOperation(operation);
            return this;
        }

        public OperationLogBuilder parameters(String parameters) {
            log.setParameters(parameters);
            return this;
        }

        public OperationLogBuilder clientInfo(String ip, String userAgent) {
            log.setClientIp(ip);
            log.setUserAgent(userAgent);
            return this;
        }

        public OperationLog build() {
            return log;
        }
    }
}
