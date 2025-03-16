package com.jobflow.service.impl;

import com.jobflow.dao.CalendarDao;
import com.jobflow.domain.Calendar;
import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import com.jobflow.service.CalendarService;
import com.jobflow.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CalendarServiceImpl extends AbstractBaseService<Calendar> implements CalendarService {

    private final CalendarDao calendarDao;
    private final NotificationService notificationService;

    @Autowired
    public CalendarServiceImpl(CalendarDao calendarDao, NotificationService notificationService) {
        super(calendarDao);
        this.calendarDao = calendarDao;
        this.notificationService = notificationService;
    }

    @Override
    public List<Calendar> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return calendarDao.findByDateRange(getCurrentTenantId(), start, end);
    }

    @Override
    public List<Calendar> findByTypeAndDateRange(Calendar.EventType type, LocalDateTime start, LocalDateTime end) {
        return calendarDao.findByTypeAndDateRange(getCurrentTenantId(), type, start, end);
    }

    @Override
    public Calendar createTaskEvent(Task task, LocalDateTime startTime, LocalDateTime endTime) {
        Calendar event = new Calendar();
        event.setTitle(task.getName());
        event.setDescription(task.getDescription());
        event.setType(Calendar.EventType.TASK);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setTask(task);
        event.setStatus(Calendar.EventStatus.SCHEDULED);
        event.setTenantId(getCurrentTenantId());
        
        if (hasSchedulingConflict(event)) {
            throw new IllegalStateException("Scheduling conflict detected");
        }

        Calendar savedEvent = save(event);
        scheduleEventReminders(savedEvent);
        return savedEvent;
    }

    @Override
    public Calendar createWorkflowEvent(Workflow workflow, LocalDateTime startTime, LocalDateTime endTime) {
        Calendar event = new Calendar();
        event.setTitle(workflow.getName());
        event.setDescription(workflow.getDescription());
        event.setType(Calendar.EventType.WORKFLOW);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setWorkflow(workflow);
        event.setStatus(Calendar.EventStatus.SCHEDULED);
        event.setTenantId(getCurrentTenantId());

        if (hasSchedulingConflict(event)) {
            throw new IllegalStateException("Scheduling conflict detected");
        }

        Calendar savedEvent = save(event);
        scheduleEventReminders(savedEvent);
        return savedEvent;
    }

    @Override
    public Calendar createRecurringEvent(Calendar event) {
        validateRecurringEvent(event);
        Calendar savedEvent = save(event);
        generateRecurringInstances(savedEvent, event.getStartTime(), event.getRecurrenceEndDate());
        return savedEvent;
    }

    @Override
    public Calendar updateEventTime(Long eventId, LocalDateTime newStart, LocalDateTime newEnd) {
        Calendar event = findById(eventId);
        event.setStartTime(newStart);
        event.setEndTime(newEnd);

        if (hasSchedulingConflict(event)) {
            throw new IllegalStateException("Scheduling conflict detected");
        }

        Calendar updatedEvent = save(event);
        updateEventReminders(updatedEvent);
        return updatedEvent;
    }

    @Override
    public Calendar updateEventRecurrence(Long eventId, Calendar.RecurrenceType type, 
                                        Integer interval, LocalDateTime endDate) {
        Calendar event = findById(eventId);
        event.setRecurrenceType(type);
        event.setRecurrenceInterval(interval);
        event.setRecurrenceEndDate(endDate);
        
        validateRecurringEvent(event);
        return save(event);
    }

    @Override
    public void cancelEvent(Long eventId) {
        Calendar event = findById(eventId);
        event.setStatus(Calendar.EventStatus.CANCELLED);
        save(event);
        notificationService.sendEventCancellationNotification(event);
    }

    @Override
    public List<Calendar> getUpcomingReminders(int minutesAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusMinutes(minutesAhead);
        return calendarDao.findUpcomingReminders(getCurrentTenantId(), now, future);
    }

    @Override
    public Map<String, Object> getEventStatistics(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalEvents = calendarDao.countEventsByDateRange(getCurrentTenantId(), start, end);
        List<Object[]> eventsByType = calendarDao.countEventsByType(getCurrentTenantId(), start, end);
        
        statistics.put("totalEvents", totalEvents);
        statistics.put("eventsByType", convertEventTypeStats(eventsByType));
        
        return statistics;
    }

    @Override
    public List<Calendar> findByStatus(Calendar.EventStatus status) {
        return calendarDao.findByStatus(getCurrentTenantId(), status, LocalDateTime.now());
    }

    @Override
    public Calendar getEventWithDetails(Long eventId) {
        return calendarDao.findByIdWithDetails(getCurrentTenantId(), eventId);
    }

    @Override
    public List<Calendar> generateRecurringInstances(Calendar event, 
                                                   LocalDateTime rangeStart, 
                                                   LocalDateTime rangeEnd) {
        if (event.getRecurrenceType() == Calendar.RecurrenceType.NONE) {
            return Collections.singletonList(event);
        }

        List<Calendar> instances = new ArrayList<>();
        LocalDateTime currentStart = event.getStartTime();
        LocalDateTime currentEnd = event.getEndTime();
        
        while (currentStart.isBefore(rangeEnd) && 
               (event.getRecurrenceEndDate() == null || currentStart.isBefore(event.getRecurrenceEndDate()))) {
            if (currentStart.isAfter(rangeStart)) {
                Calendar instance = createEventInstance(event, currentStart, currentEnd);
                instances.add(instance);
            }
            
            switch (event.getRecurrenceType()) {
                case DAILY:
                    currentStart = currentStart.plusDays(event.getRecurrenceInterval());
                    currentEnd = currentEnd.plusDays(event.getRecurrenceInterval());
                    break;
                case WEEKLY:
                    currentStart = currentStart.plusWeeks(event.getRecurrenceInterval());
                    currentEnd = currentEnd.plusWeeks(event.getRecurrenceInterval());
                    break;
                case MONTHLY:
                    currentStart = currentStart.plusMonths(event.getRecurrenceInterval());
                    currentEnd = currentEnd.plusMonths(event.getRecurrenceInterval());
                    break;
                case YEARLY:
                    currentStart = currentStart.plusYears(event.getRecurrenceInterval());
                    currentEnd = currentEnd.plusYears(event.getRecurrenceInterval());
                    break;
            }
        }
        
        return instances;
    }

    @Override
    public boolean hasSchedulingConflict(Calendar event) {
        List<Calendar> existingEvents = findByDateRange(event.getStartTime(), event.getEndTime());
        return existingEvents.stream()
            .filter(e -> !e.getId().equals(event.getId()))
            .anyMatch(e -> eventsOverlap(event, e));
    }

    @Override
    public List<Calendar> getTaskEvents(Long taskId) {
        return calendarDao.findByTaskId(getCurrentTenantId(), taskId);
    }

    @Override
    public List<Calendar> getWorkflowEvents(Long workflowId) {
        return calendarDao.findByWorkflowId(getCurrentTenantId(), workflowId);
    }

    @Override
    public Calendar updateEventStatus(Long eventId, Calendar.EventStatus status) {
        Calendar event = findById(eventId);
        event.setStatus(status);
        return save(event);
    }

    @Override
    public Calendar setEventReminder(Long eventId, Integer reminderMinutes) {
        Calendar event = findById(eventId);
        event.setReminderMinutes(reminderMinutes);
        Calendar updatedEvent = save(event);
        scheduleEventReminders(updatedEvent);
        return updatedEvent;
    }

    @Override
    public Map<Calendar.EventType, Long> getEventCountByType(LocalDateTime start, LocalDateTime end) {
        List<Object[]> counts = calendarDao.countEventsByType(getCurrentTenantId(), start, end);
        return counts.stream()
            .collect(Collectors.toMap(
                row -> (Calendar.EventType) row[0],
                row -> (Long) row[1]
            ));
    }

    @Override
    public Map<String, Object> getDashboardEvents() {
        Map<String, Object> dashboard = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        dashboard.put("todayEvents", findByDateRange(
            now.truncatedTo(ChronoUnit.DAYS),
            now.truncatedTo(ChronoUnit.DAYS).plusDays(1)
        ));
        
        dashboard.put("upcomingEvents", findByDateRange(
            now,
            now.plusDays(7)
        ));
        
        dashboard.put("statistics", getEventStatistics(
            now.minusMonths(1),
            now
        ));
        
        return dashboard;
    }

    // Helper methods
    private void validateRecurringEvent(Calendar event) {
        if (event.getRecurrenceType() != Calendar.RecurrenceType.NONE) {
            if (event.getRecurrenceInterval() == null || event.getRecurrenceInterval() < 1) {
                throw new IllegalArgumentException("Invalid recurrence interval");
            }
        }
    }

    private Calendar createEventInstance(Calendar template, LocalDateTime start, LocalDateTime end) {
        Calendar instance = new Calendar();
        instance.setTitle(template.getTitle());
        instance.setDescription(template.getDescription());
        instance.setType(template.getType());
        instance.setStartTime(start);
        instance.setEndTime(end);
        instance.setAllDay(template.getAllDay());
        instance.setColor(template.getColor());
        instance.setLocation(template.getLocation());
        instance.setReminderMinutes(template.getReminderMinutes());
        instance.setStatus(Calendar.EventStatus.SCHEDULED);
        instance.setTenantId(getCurrentTenantId());
        return save(instance);
    }

    private boolean eventsOverlap(Calendar event1, Calendar event2) {
        return !event1.getEndTime().isBefore(event2.getStartTime()) && 
               !event2.getEndTime().isBefore(event1.getStartTime());
    }

    private void scheduleEventReminders(Calendar event) {
        if (event.getReminderMinutes() != null && event.getReminderMinutes() > 0) {
            LocalDateTime reminderTime = event.getStartTime()
                .minusMinutes(event.getReminderMinutes());
            if (reminderTime.isAfter(LocalDateTime.now())) {
                notificationService.scheduleEventReminder(event, reminderTime);
            }
        }
    }

    private void updateEventReminders(Calendar event) {
        notificationService.cancelEventReminders(event);
        scheduleEventReminders(event);
    }

    private Map<String, Long> convertEventTypeStats(List<Object[]> eventsByType) {
        return eventsByType.stream()
            .collect(Collectors.toMap(
                row -> ((Calendar.EventType) row[0]).name(),
                row -> (Long) row[1]
            ));
    }
}
