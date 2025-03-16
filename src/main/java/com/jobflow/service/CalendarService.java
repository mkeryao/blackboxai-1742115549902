package com.jobflow.service;

import com.jobflow.domain.Calendar;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CalendarService extends BaseService<Calendar> {

    /**
     * Find events within a date range
     */
    List<Calendar> findByDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Find events by type within a date range
     */
    List<Calendar> findByTypeAndDateRange(Calendar.EventType type, LocalDateTime start, LocalDateTime end);

    /**
     * Create an event for a task
     */
    Calendar createTaskEvent(Task task, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Create an event for a workflow
     */
    Calendar createWorkflowEvent(Workflow workflow, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Create a recurring event
     */
    Calendar createRecurringEvent(Calendar event);

    /**
     * Update event time
     */
    Calendar updateEventTime(Long eventId, LocalDateTime newStart, LocalDateTime newEnd);

    /**
     * Update event recurrence
     */
    Calendar updateEventRecurrence(Long eventId, Calendar.RecurrenceType type, Integer interval, LocalDateTime endDate);

    /**
     * Cancel event
     */
    void cancelEvent(Long eventId);

    /**
     * Get upcoming events with reminders
     */
    List<Calendar> getUpcomingReminders(int minutesAhead);

    /**
     * Get event statistics
     */
    Map<String, Object> getEventStatistics(LocalDateTime start, LocalDateTime end);

    /**
     * Get events by status
     */
    List<Calendar> findByStatus(Calendar.EventStatus status);

    /**
     * Get event with all details
     */
    Calendar getEventWithDetails(Long eventId);

    /**
     * Generate recurring event instances
     */
    List<Calendar> generateRecurringInstances(Calendar event, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    /**
     * Check for scheduling conflicts
     */
    boolean hasSchedulingConflict(Calendar event);

    /**
     * Get task-related events
     */
    List<Calendar> getTaskEvents(Long taskId);

    /**
     * Get workflow-related events
     */
    List<Calendar> getWorkflowEvents(Long workflowId);

    /**
     * Update event status
     */
    Calendar updateEventStatus(Long eventId, Calendar.EventStatus status);

    /**
     * Set event reminder
     */
    Calendar setEventReminder(Long eventId, Integer reminderMinutes);

    /**
     * Get event count by type
     */
    Map<Calendar.EventType, Long> getEventCountByType(LocalDateTime start, LocalDateTime end);

    /**
     * Get events for dashboard
     */
    Map<String, Object> getDashboardEvents();
}
