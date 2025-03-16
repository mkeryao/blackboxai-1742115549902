package com.jobflow.controller;

import com.jobflow.domain.Calendar;
import com.jobflow.service.CalendarService;
import com.jobflow.service.TaskService;
import com.jobflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar Management", description = "Calendar management APIs")
public class CalendarController extends BaseController {

    private final CalendarService calendarService;
    private final TaskService taskService;
    private final WorkflowService workflowService;

    @Operation(summary = "Get events by date range")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<Calendar>>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Calendar> events = calendarService.findByDateRange(start, end);
        return success(events);
    }

    @Operation(summary = "Get events by type and date range")
    @GetMapping("/events/type/{type}")
    public ResponseEntity<ApiResponse<List<Calendar>>> getEventsByType(
            @PathVariable Calendar.EventType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Calendar> events = calendarService.findByTypeAndDateRange(type, start, end);
        return success(events);
    }

    @Operation(summary = "Create a new event")
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Calendar>> createEvent(@Valid @RequestBody Calendar event) {
        Calendar createdEvent = calendarService.save(event);
        return success(createdEvent);
    }

    @Operation(summary = "Create a task event")
    @PostMapping("/events/task/{taskId}")
    public ResponseEntity<ApiResponse<Calendar>> createTaskEvent(
            @PathVariable Long taskId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Calendar event = calendarService.createTaskEvent(taskService.findById(taskId), start, end);
        return success(event);
    }

    @Operation(summary = "Create a workflow event")
    @PostMapping("/events/workflow/{workflowId}")
    public ResponseEntity<ApiResponse<Calendar>> createWorkflowEvent(
            @PathVariable Long workflowId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Calendar event = calendarService.createWorkflowEvent(workflowService.findById(workflowId), start, end);
        return success(event);
    }

    @Operation(summary = "Create a recurring event")
    @PostMapping("/events/recurring")
    public ResponseEntity<ApiResponse<Calendar>> createRecurringEvent(@Valid @RequestBody Calendar event) {
        Calendar createdEvent = calendarService.createRecurringEvent(event);
        return success(createdEvent);
    }

    @Operation(summary = "Update event time")
    @PutMapping("/events/{eventId}/time")
    public ResponseEntity<ApiResponse<Calendar>> updateEventTime(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Calendar updatedEvent = calendarService.updateEventTime(eventId, start, end);
        return success(updatedEvent);
    }

    @Operation(summary = "Update event recurrence")
    @PutMapping("/events/{eventId}/recurrence")
    public ResponseEntity<ApiResponse<Calendar>> updateEventRecurrence(
            @PathVariable Long eventId,
            @RequestParam Calendar.RecurrenceType type,
            @RequestParam Integer interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Calendar updatedEvent = calendarService.updateEventRecurrence(eventId, type, interval, endDate);
        return success(updatedEvent);
    }

    @Operation(summary = "Cancel event")
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Void>> cancelEvent(@PathVariable Long eventId) {
        calendarService.cancelEvent(eventId);
        return success();
    }

    @Operation(summary = "Get upcoming reminders")
    @GetMapping("/events/reminders")
    public ResponseEntity<ApiResponse<List<Calendar>>> getUpcomingReminders(
            @RequestParam(defaultValue = "30") Integer minutesAhead) {
        List<Calendar> reminders = calendarService.getUpcomingReminders(minutesAhead);
        return success(reminders);
    }

    @Operation(summary = "Get event statistics")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Object> statistics = calendarService.getEventStatistics(start, end);
        return success(statistics);
    }

    @Operation(summary = "Get events by status")
    @GetMapping("/events/status/{status}")
    public ResponseEntity<ApiResponse<List<Calendar>>> getEventsByStatus(
            @PathVariable Calendar.EventStatus status) {
        List<Calendar> events = calendarService.findByStatus(status);
        return success(events);
    }

    @Operation(summary = "Get event details")
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Calendar>> getEventDetails(@PathVariable Long eventId) {
        Calendar event = calendarService.getEventWithDetails(eventId);
        return success(event);
    }

    @Operation(summary = "Check scheduling conflicts")
    @PostMapping("/events/check-conflicts")
    public ResponseEntity<ApiResponse<Boolean>> checkSchedulingConflicts(@RequestBody Calendar event) {
        boolean hasConflicts = calendarService.hasSchedulingConflict(event);
        return success(hasConflicts);
    }

    @Operation(summary = "Get task events")
    @GetMapping("/events/task/{taskId}")
    public ResponseEntity<ApiResponse<List<Calendar>>> getTaskEvents(@PathVariable Long taskId) {
        List<Calendar> events = calendarService.getTaskEvents(taskId);
        return success(events);
    }

    @Operation(summary = "Get workflow events")
    @GetMapping("/events/workflow/{workflowId}")
    public ResponseEntity<ApiResponse<List<Calendar>>> getWorkflowEvents(@PathVariable Long workflowId) {
        List<Calendar> events = calendarService.getWorkflowEvents(workflowId);
        return success(events);
    }

    @Operation(summary = "Update event status")
    @PutMapping("/events/{eventId}/status")
    public ResponseEntity<ApiResponse<Calendar>> updateEventStatus(
            @PathVariable Long eventId,
            @RequestParam Calendar.EventStatus status) {
        Calendar updatedEvent = calendarService.updateEventStatus(eventId, status);
        return success(updatedEvent);
    }

    @Operation(summary = "Set event reminder")
    @PutMapping("/events/{eventId}/reminder")
    public ResponseEntity<ApiResponse<Calendar>> setEventReminder(
            @PathVariable Long eventId,
            @RequestParam Integer reminderMinutes) {
        Calendar updatedEvent = calendarService.setEventReminder(eventId, reminderMinutes);
        return success(updatedEvent);
    }

    @Operation(summary = "Get event count by type")
    @GetMapping("/events/count-by-type")
    public ResponseEntity<ApiResponse<Map<Calendar.EventType, Long>>> getEventCountByType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<Calendar.EventType, Long> counts = calendarService.getEventCountByType(start, end);
        return success(counts);
    }

    @Operation(summary = "Get dashboard events")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardEvents() {
        Map<String, Object> dashboard = calendarService.getDashboardEvents();
        return success(dashboard);
    }
}
