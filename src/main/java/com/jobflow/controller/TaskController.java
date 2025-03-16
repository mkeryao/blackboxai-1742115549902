package com.jobflow.controller;

import com.jobflow.domain.Task;
import com.jobflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Task Controller
 * 
 * Handles task-related operations including CRUD and execution.
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController extends BaseController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a new task")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Task>> createTask(
            @Valid @RequestBody Task task) {
        try {
            task.setTenantId(getCurrentTenantId());
            Task createdTask = taskService.create(task, getCurrentUser().getUsername());
            return success(createdTask, "Task created successfully");
        } catch (Exception e) {
            log.error("Failed to create task", e);
            return error("Failed to create task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update an existing task")
    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Task>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody Task task) {
        try {
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            task.setId(taskId);
            Task updatedTask = taskService.update(task, getCurrentUser().getUsername());
            return success(updatedTask, "Task updated successfully");
        } catch (Exception e) {
            log.error("Failed to update task", e);
            return error("Failed to update task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            taskService.delete(taskId, getCurrentUser().getUsername());
            return success(null, "Task deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete task", e);
            return error("Failed to delete task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get task by ID")
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Task>> getTask(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            return success(task);
        } catch (Exception e) {
            log.error("Failed to get task", e);
            return error("Failed to get task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get tasks by group name")
    @GetMapping("/group/{groupName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByGroup(
            @PathVariable String groupName) {
        try {
            List<Task> tasks = taskService.findByGroupName(groupName, getCurrentTenantId());
            return success(tasks);
        } catch (Exception e) {
            log.error("Failed to get tasks by group", e);
            return error("Failed to get tasks by group: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Execute a task")
    @PostMapping("/{taskId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> executeTask(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            taskService.executeTask(task, getCurrentUser().getUsername());
            return success(null, "Task execution started");
        } catch (Exception e) {
            log.error("Failed to execute task", e);
            return error("Failed to execute task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Retry a failed task")
    @PostMapping("/{taskId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> retryTask(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            taskService.retryTask(taskId, getCurrentUser().getUsername());
            return success(null, "Task retry started");
        } catch (Exception e) {
            log.error("Failed to retry task", e);
            return error("Failed to retry task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Cancel a running task")
    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> cancelTask(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            taskService.cancelTask(taskId, getCurrentUser().getUsername());
            return success(null, "Task cancelled successfully");
        } catch (Exception e) {
            log.error("Failed to cancel task", e);
            return error("Failed to cancel task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get task execution history")
    @GetMapping("/{taskId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTaskHistory(
            @PathVariable Long taskId) {
        try {
            Task task = taskService.findById(taskId);
            verifyResourceAccess(task.getCreatedBy(), task.getTenantId());
            List<Task> history = taskService.getExecutionHistory(taskId, getCurrentTenantId());
            return success(history);
        } catch (Exception e) {
            log.error("Failed to get task history", e);
            return error("Failed to get task history: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get task statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TaskService.TaskStatistics>> getTaskStatistics() {
        try {
            TaskService.TaskStatistics statistics = taskService.getTaskStatistics(getCurrentTenantId());
            return success(statistics);
        } catch (Exception e) {
            log.error("Failed to get task statistics", e);
            return error("Failed to get task statistics: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
