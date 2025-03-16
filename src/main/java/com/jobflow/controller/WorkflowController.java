package com.jobflow.controller;

import com.jobflow.domain.Task;
import com.jobflow.domain.Workflow;
import com.jobflow.domain.WorkflowDependency;
import com.jobflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Workflow Controller
 * 
 * Handles workflow-related operations including CRUD, execution, and dependency management.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@Tag(name = "Workflow Management", description = "APIs for managing workflows")
public class WorkflowController extends BaseController {

    private final WorkflowService workflowService;

    @Autowired
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Operation(summary = "Create a new workflow")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Workflow>> createWorkflow(
            @Valid @RequestBody Workflow workflow) {
        try {
            workflow.setTenantId(getCurrentTenantId());
            Workflow createdWorkflow = workflowService.create(workflow, getCurrentUser().getUsername());
            return success(createdWorkflow, "Workflow created successfully");
        } catch (Exception e) {
            log.error("Failed to create workflow", e);
            return error("Failed to create workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update an existing workflow")
    @PutMapping("/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Workflow>> updateWorkflow(
            @PathVariable Long workflowId,
            @Valid @RequestBody Workflow workflow) {
        try {
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflow.setId(workflowId);
            Workflow updatedWorkflow = workflowService.update(workflow, getCurrentUser().getUsername());
            return success(updatedWorkflow, "Workflow updated successfully");
        } catch (Exception e) {
            log.error("Failed to update workflow", e);
            return error("Failed to update workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Delete a workflow")
    @DeleteMapping("/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.delete(workflowId, getCurrentUser().getUsername());
            return success(null, "Workflow deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete workflow", e);
            return error("Failed to delete workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get workflow by ID")
    @GetMapping("/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Workflow>> getWorkflow(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            return success(workflow);
        } catch (Exception e) {
            log.error("Failed to get workflow", e);
            return error("Failed to get workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Execute a workflow")
    @PostMapping("/{workflowId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> executeWorkflow(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.executeWorkflow(workflow, getCurrentUser().getUsername());
            return success(null, "Workflow execution started");
        } catch (Exception e) {
            log.error("Failed to execute workflow", e);
            return error("Failed to execute workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Execute workflow from a specific task")
    @PostMapping("/{workflowId}/execute/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> executeWorkflowFromTask(
            @PathVariable Long workflowId,
            @PathVariable Long taskId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.executeWorkflowFromTask(workflowId, taskId, getCurrentUser().getUsername());
            return success(null, "Workflow execution started from specified task");
        } catch (Exception e) {
            log.error("Failed to execute workflow from task", e);
            return error("Failed to execute workflow from task: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Cancel a running workflow")
    @PostMapping("/{workflowId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> cancelWorkflow(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.cancelWorkflow(workflowId, getCurrentUser().getUsername());
            return success(null, "Workflow cancelled successfully");
        } catch (Exception e) {
            log.error("Failed to cancel workflow", e);
            return error("Failed to cancel workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Add task to workflow")
    @PostMapping("/{workflowId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> addTask(
            @PathVariable Long workflowId,
            @Valid @RequestBody Task task) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.addTask(workflowId, task, getCurrentUser().getUsername());
            return success(null, "Task added to workflow successfully");
        } catch (Exception e) {
            log.error("Failed to add task to workflow", e);
            return error("Failed to add task to workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Remove task from workflow")
    @DeleteMapping("/{workflowId}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeTask(
            @PathVariable Long workflowId,
            @PathVariable Long taskId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.removeTask(workflowId, taskId, getCurrentUser().getUsername());
            return success(null, "Task removed from workflow successfully");
        } catch (Exception e) {
            log.error("Failed to remove task from workflow", e);
            return error("Failed to remove task from workflow: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Add dependency between tasks")
    @PostMapping("/{workflowId}/dependencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> addDependency(
            @PathVariable Long workflowId,
            @RequestParam Long sourceTaskId,
            @RequestParam Long targetTaskId,
            @RequestParam WorkflowDependency.DependencyType type) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.addDependency(workflowId, sourceTaskId, targetTaskId, type, 
                                        getCurrentUser().getUsername());
            return success(null, "Dependency added successfully");
        } catch (Exception e) {
            log.error("Failed to add dependency", e);
            return error("Failed to add dependency: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Remove dependency between tasks")
    @DeleteMapping("/{workflowId}/dependencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeDependency(
            @PathVariable Long workflowId,
            @RequestParam Long sourceTaskId,
            @RequestParam Long targetTaskId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            workflowService.removeDependency(workflowId, sourceTaskId, targetTaskId, 
                                           getCurrentUser().getUsername());
            return success(null, "Dependency removed successfully");
        } catch (Exception e) {
            log.error("Failed to remove dependency", e);
            return error("Failed to remove dependency: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get workflow tasks")
    @GetMapping("/{workflowId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<Task>>> getWorkflowTasks(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            List<Task> tasks = workflowService.getWorkflowTasks(workflowId);
            return success(tasks);
        } catch (Exception e) {
            log.error("Failed to get workflow tasks", e);
            return error("Failed to get workflow tasks: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get workflow dependencies")
    @GetMapping("/{workflowId}/dependencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<WorkflowDependency>>> getWorkflowDependencies(
            @PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.findById(workflowId);
            verifyResourceAccess(workflow.getCreatedBy(), workflow.getTenantId());
            List<WorkflowDependency> dependencies = workflowService.getWorkflowDependencies(workflowId);
            return success(dependencies);
        } catch (Exception e) {
            log.error("Failed to get workflow dependencies", e);
            return error("Failed to get workflow dependencies: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get workflow statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<WorkflowService.WorkflowStatistics>> getWorkflowStatistics() {
        try {
            WorkflowService.WorkflowStatistics statistics = 
                workflowService.getWorkflowStatistics(getCurrentTenantId());
            return success(statistics);
        } catch (Exception e) {
            log.error("Failed to get workflow statistics", e);
            return error("Failed to get workflow statistics: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
