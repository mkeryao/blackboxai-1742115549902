package com.jobflow.dao;

import com.jobflow.domain.Calendar;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarDao extends BaseDao<Calendar> {

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND ((c.startTime BETWEEN :start AND :end) OR (c.endTime BETWEEN :start AND :end))")
    List<Calendar> findByDateRange(@Param("tenantId") Long tenantId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.type = :type " +
           "AND ((c.startTime BETWEEN :start AND :end) OR (c.endTime BETWEEN :start AND :end))")
    List<Calendar> findByTypeAndDateRange(@Param("tenantId") Long tenantId,
                                        @Param("type") Calendar.EventType type,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.task.id = :taskId")
    List<Calendar> findByTaskId(@Param("tenantId") Long tenantId,
                              @Param("taskId") Long taskId);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.workflow.id = :workflowId")
    List<Calendar> findByWorkflowId(@Param("tenantId") Long tenantId,
                                  @Param("workflowId") Long workflowId);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.recurrenceType != com.jobflow.domain.Calendar.RecurrenceType.NONE " +
           "AND (c.recurrenceEndDate IS NULL OR c.recurrenceEndDate >= :now)")
    List<Calendar> findRecurringEvents(@Param("tenantId") Long tenantId,
                                     @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.reminderMinutes IS NOT NULL " +
           "AND c.startTime BETWEEN :start AND :end")
    List<Calendar> findUpcomingReminders(@Param("tenantId") Long tenantId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.startTime >= :start AND c.startTime < :end")
    long countEventsByDateRange(@Param("tenantId") Long tenantId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT c.type, COUNT(c) FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.startTime >= :start AND c.startTime < :end " +
           "GROUP BY c.type")
    List<Object[]> countEventsByType(@Param("tenantId") Long tenantId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Calendar c WHERE c.tenantId = :tenantId " +
           "AND c.status = :status " +
           "AND c.startTime >= :start")
    List<Calendar> findByStatus(@Param("tenantId") Long tenantId,
                              @Param("status") Calendar.EventStatus status,
                              @Param("start") LocalDateTime start);

    @Query("SELECT DISTINCT c FROM Calendar c " +
           "LEFT JOIN FETCH c.task t " +
           "LEFT JOIN FETCH c.workflow w " +
           "WHERE c.tenantId = :tenantId " +
           "AND c.id = :id")
    Calendar findByIdWithDetails(@Param("tenantId") Long tenantId,
                               @Param("id") Long id);
}
