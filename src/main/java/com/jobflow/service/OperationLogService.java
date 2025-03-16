package com.jobflow.service;

import com.jobflow.domain.OperationLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing operation logs
 */
public interface OperationLogService extends BaseService<OperationLog> {
    
    /**
     * Find logs by time range
     * @param startTime Start time
     * @param endTime End time
     * @param tenantId Tenant ID
     * @return List of operation logs
     */
    List<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Long tenantId);

    /**
     * Find logs by operator
     * @param operatorId Operator ID
     * @param tenantId Tenant ID
     * @return List of operation logs
     */
    List<OperationLog> findByOperator(Long operatorId, Long tenantId);

    /**
     * Find logs by module and resource
     * @param module Operation module
     * @param resourceId Resource ID
     * @param tenantId Tenant ID
     * @return List of operation logs
     */
    List<OperationLog> findByModuleAndResource(OperationLog.OperationModule module, 
                                             String resourceId, Long tenantId);

    /**
     * Find logs by operation type
     * @param type Operation type
     * @param tenantId Tenant ID
     * @return List of operation logs
     */
    List<OperationLog> findByType(OperationLog.OperationType type, Long tenantId);

    /**
     * Find logs by status
     * @param status Operation status
     * @param tenantId Tenant ID
     * @return List of operation logs
     */
    List<OperationLog> findByStatus(OperationLog.OperationStatus status, Long tenantId);

    /**
     * Get operation log statistics
     * @param tenantId Tenant ID
     * @return Operation log statistics
     */
    OperationLogStatistics getOperationLogStatistics(Long tenantId);

    /**
     * Get operation trends
     * @param tenantId Tenant ID
     * @param days Number of days to analyze
     * @return Operation trends data
     */
    OperationTrends getOperationTrends(Long tenantId, int days);

    /**
     * Export operation logs
     * @param startTime Start time
     * @param endTime End time
     * @param tenantId Tenant ID
     * @return Exported file content
     */
    byte[] exportOperationLogs(LocalDateTime startTime, LocalDateTime endTime, Long tenantId);

    /**
     * Inner class for operation log statistics
     */
    class OperationLogStatistics {
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private Map<OperationLog.OperationType, Long> operationsByType;
        private Map<OperationLog.OperationModule, Long> operationsByModule;
        private Map<String, Long> operationsByOperator;
        private double averageOperationDuration;
        private double successRate;
        private LocalDateTime lastOperationTime;

        // Getters and setters
        public long getTotalOperations() { return totalOperations; }
        public void setTotalOperations(long totalOperations) { 
            this.totalOperations = totalOperations; 
        }
        
        public long getSuccessfulOperations() { return successfulOperations; }
        public void setSuccessfulOperations(long successfulOperations) { 
            this.successfulOperations = successfulOperations; 
        }
        
        public long getFailedOperations() { return failedOperations; }
        public void setFailedOperations(long failedOperations) { 
            this.failedOperations = failedOperations; 
        }
        
        public Map<OperationLog.OperationType, Long> getOperationsByType() { 
            return operationsByType; 
        }
        public void setOperationsByType(Map<OperationLog.OperationType, Long> operationsByType) { 
            this.operationsByType = operationsByType; 
        }
        
        public Map<OperationLog.OperationModule, Long> getOperationsByModule() { 
            return operationsByModule; 
        }
        public void setOperationsByModule(Map<OperationLog.OperationModule, Long> operationsByModule) { 
            this.operationsByModule = operationsByModule; 
        }
        
        public Map<String, Long> getOperationsByOperator() { return operationsByOperator; }
        public void setOperationsByOperator(Map<String, Long> operationsByOperator) { 
            this.operationsByOperator = operationsByOperator; 
        }
        
        public double getAverageOperationDuration() { return averageOperationDuration; }
        public void setAverageOperationDuration(double averageOperationDuration) { 
            this.averageOperationDuration = averageOperationDuration; 
        }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public LocalDateTime getLastOperationTime() { return lastOperationTime; }
        public void setLastOperationTime(LocalDateTime lastOperationTime) { 
            this.lastOperationTime = lastOperationTime; 
        }
    }

    /**
     * Inner class for operation trends
     */
    class OperationTrends {
        private List<LocalDateTime> timePoints;
        private Map<OperationLog.OperationType, List<Long>> operationCounts;
        private Map<OperationLog.OperationModule, List<Long>> moduleCounts;
        private List<Double> successRates;
        private List<Double> averageDurations;

        // Getters and setters
        public List<LocalDateTime> getTimePoints() { return timePoints; }
        public void setTimePoints(List<LocalDateTime> timePoints) { this.timePoints = timePoints; }
        
        public Map<OperationLog.OperationType, List<Long>> getOperationCounts() { 
            return operationCounts; 
        }
        public void setOperationCounts(Map<OperationLog.OperationType, List<Long>> operationCounts) { 
            this.operationCounts = operationCounts; 
        }
        
        public Map<OperationLog.OperationModule, List<Long>> getModuleCounts() { 
            return moduleCounts; 
        }
        public void setModuleCounts(Map<OperationLog.OperationModule, List<Long>> moduleCounts) { 
            this.moduleCounts = moduleCounts; 
        }
        
        public List<Double> getSuccessRates() { return successRates; }
        public void setSuccessRates(List<Double> successRates) { this.successRates = successRates; }
        
        public List<Double> getAverageDurations() { return averageDurations; }
        public void setAverageDurations(List<Double> averageDurations) { 
            this.averageDurations = averageDurations; 
        }
    }
}
