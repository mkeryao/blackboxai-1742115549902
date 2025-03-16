package com.jobflow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fj_calendar")
@EqualsAndHashCode(callSuper = true)
public class Calendar extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "all_day")
    private Boolean allDay;

    @Column(name = "recurrence_type")
    @Enumerated(EnumType.STRING)
    private RecurrenceType recurrenceType;

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(name = "recurrence_end_date")
    private LocalDateTime recurrenceEndDate;

    @Column(name = "color")
    private String color;

    @Column(name = "location")
    private String location;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    public enum EventType {
        TASK,
        WORKFLOW,
        MEETING,
        REMINDER,
        OTHER
    }

    public enum RecurrenceType {
        NONE,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }

    public enum EventStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
