package com.jobflow.dao;

import com.jobflow.domain.ExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ExecutionRecordDao extends BaseDao<ExecutionRecord> {

    @Query("SELECT e FROM ExecutionRecord e WHERE e.tenantId = :tenantId " +
           "AND e.type = :type " +
           "AND e.task.id = :resourceId")
    List<ExecutionRecord> findTaskExecutions(@Param("tenantId") Long tenantId,
                                           @Param("type") ExecutionRecord.ExecutionType type,
                                           @Param("resourceId") Long resourceId);

    @Query("SELECT e FROM ExecutionRecord e WHERE e.tenantId = :tenantId " +
           "AND e.type = :type " +
           "AND e.workflow.id = :resourceId")
    List<ExecutionRecord> findWorkflowExecutions(@Param("tenantId") Long tenantId,
                                                @Param("type") ExecutionRecord.ExecutionType type,
                                                @Param("resourceId") Long resourceId);

    @Query("SELECT e FROM ExecutionRecord e WHERE e.tenantId = :tenantId " +
           "AND e.status = :status " +
           "AND e.nextRetryTime <= :now")
    List<ExecutionRecord> findRetryableExecutions(@Param("tenantId") Long tenantId,
                                                 @Param("status") ExecutionRecord.ExecutionStatus status,
                                                 @Param("now") LocalDateTime now);

    @Query("SELECT e FROM ExecutionRecord e WHERE e.tenantId = :tenantId " +
           "AND e.status IN :statuses " +
           "AND e.startTime >= :startTime")
    List<ExecutionRecord> findByStatusAndStartTime(@Param("tenantId") Long tenantId,
                                                  @Param("statuses") List<ExecutionRecord.ExecutionStatus> statuses,
                                                  @Param("startTime") LocalDateTime startTime);

    @Query("SELECT e.status, COUNT(e) FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.startTime BETWEEN :start AND :end " +
           "GROUP BY e.status")
    List<Object[]> getExecutionStatistics(@Param("tenantId") Long tenantId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT AVG(e.duration) FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.type = :type " +
           "AND e.status = 'COMPLETED' " +
           "AND e.startTime BETWEEN :start AND :end")
    Double getAverageExecutionTime(@Param("tenantId") Long tenantId,
                                 @Param("type") ExecutionRecord.ExecutionType type,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT e FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.status = 'RUNNING' " +
           "AND e.startTime <= :timeout")
    List<ExecutionRecord> findTimedOutExecutions(@Param("tenantId") Long tenantId,
                                                @Param("timeout") LocalDateTime timeout);

    @Query("SELECT e FROM ExecutionRecord e " +
           "LEFT JOIN FETCH e.task t " +
           "LEFT JOIN FETCH e.workflow w " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.executionId = :executionId")
    ExecutionRecord findByExecutionId(@Param("tenantId") Long tenantId,
                                    @Param("executionId") String executionId);

    @Query("SELECT e FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND (:type IS NULL OR e.type = :type) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:startTime IS NULL OR e.startTime >= :startTime) " +
           "AND (:endTime IS NULL OR e.startTime <= :endTime) " +
           "AND (:resourceId IS NULL OR (e.type = 'TASK' AND e.task.id = :resourceId) " +
           "     OR (e.type = 'WORKFLOW' AND e.workflow.id = :resourceId))")
    Page<ExecutionRecord> searchExecutions(@Param("tenantId") Long tenantId,
                                         @Param("type") ExecutionRecord.ExecutionType type,
                                         @Param("status") ExecutionRecord.ExecutionStatus status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("resourceId") Long resourceId,
                                         Pageable pageable);

    @Query("SELECT new map(" +
           "e.type as type, " +
           "e.status as status, " +
           "COUNT(e) as count, " +
           "AVG(e.duration) as avgDuration, " +
           "MIN(e.duration) as minDuration, " +
           "MAX(e.duration) as maxDuration) " +
           "FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.startTime BETWEEN :start AND :end " +
           "GROUP BY e.type, e.status")
    List<Map<String, Object>> getDetailedStatistics(@Param("tenantId") Long tenantId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) > 0 FROM ExecutionRecord e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.type = :type " +
           "AND e.status = 'RUNNING' " +
           "AND (:taskId IS NULL OR e.task.id = :taskId) " +
           "AND (:workflowId IS NULL OR e.workflow.id = :workflowId)")
    boolean hasRunningExecution(@Param("tenantId") Long tenantId,
                              @Param("type") ExecutionRecord.ExecutionType type,
                              @Param("taskId") Long taskId,
                              @Param("workflowId") Long workflowId);
}
