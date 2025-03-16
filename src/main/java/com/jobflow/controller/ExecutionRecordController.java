package com.jobflow.controller;

import com.jobflow.domain.ExecutionRecord;
import com.jobflow.service.ExecutionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/execution-records")
@RequiredArgsConstructor
@Tag(name = "Execution Record Management", description = "APIs for managing execution records")
public class ExecutionRecordController extends BaseController {

    private final ExecutionRecordService executionRecordService;

    @Operation(summary = "Create a new task execution record")
    @PostMapping("/task")
    public ResponseEntity<ApiResponse<ExecutionRecord>> createTaskExecution(@RequestBody ExecutionRecord executionRecord) {
        ExecutionRecord createdRecord = executionRecordService.createTaskExecution(executionRecord.getTask(), 
            executionRecord.getExecutor(), executionRecord.getExecutorIp(), 
            executionRecord.getTriggerType(), executionRecord.getTriggerInfo());
        return success(createdRecord);
    }

    @Operation(summary = "Create a new workflow execution record")
    @PostMapping("/workflow")
    public ResponseEntity<ApiResponse<ExecutionRecord>> createWorkflowExecution(@RequestBody ExecutionRecord executionRecord) {
        ExecutionRecord createdRecord = executionRecordService.createWorkflowExecution(executionRecord.getWorkflow(), 
            executionRecord.getExecutor(), executionRecord.getExecutorIp(), 
            executionRecord.getTriggerType(), executionRecord.getTriggerInfo());
        return success(createdRecord);
    }

    @Operation(summary = "Get task execution history")
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<List<ExecutionRecord>>> getTaskExecutions(@PathVariable Long taskId) {
        List<ExecutionRecord> records = executionRecordService.getTaskExecutions(taskId);
        return success(records);
    }

    @Operation(summary = "Get workflow execution history")
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<ApiResponse<List<ExecutionRecord>>> getWorkflowExecutions(@PathVariable Long workflowId) {
        List<ExecutionRecord> records = executionRecordService.getWorkflowExecutions(workflowId);
        return success(records);
    }

    @Operation(summary = "Get retryable executions")
    @GetMapping("/retryable")
    public ResponseEntity<ApiResponse<List<ExecutionRecord>>> findRetryableExecutions() {
        List<ExecutionRecord> records = executionRecordService.findRetryableExecutions();
        return success(records);
    }

    @Operation(summary = "Get execution statistics")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<ExecutionRecord.ExecutionStatus, Long>>> getExecutionStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<ExecutionRecord.ExecutionStatus, Long> statistics = executionRecordService.getExecutionStatistics(start, end);
        return success(statistics);
    }

    @Operation(summary = "Get average execution time")
    @GetMapping("/average-time")
    public ResponseEntity<ApiResponse<Double>> getAverageExecutionTime(
            @RequestParam ExecutionRecord.ExecutionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Double averageTime = executionRecordService.getAverageExecutionTime(type, start, end);
        return success(averageTime);
    }

    @Operation(summary = "Find timed out executions")
    @GetMapping("/timed-out")
    public ResponseEntity<ApiResponse<List<ExecutionRecord>>> findTimedOutExecutions(
            @RequestParam(defaultValue = "30") int timeoutMinutes) {
        List<ExecutionRecord> records = executionRecordService.findTimedOutExecutions(timeoutMinutes);
        return success(records);
    }

    @Operation(summary = "Get execution record by execution ID")
    @GetMapping("/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> getByExecutionId(@PathVariable String executionId) {
        ExecutionRecord record = executionRecordService.getByExecutionId(executionId);
        return success(record);
    }

    @Operation(summary = "Search executions with filters")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ExecutionRecord>>> searchExecutions(
            @RequestParam(required = false) ExecutionRecord.ExecutionType type,
            @RequestParam(required = false) ExecutionRecord.ExecutionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long resourceId,
            Pageable pageable) {
        Page<ExecutionRecord> records = executionRecordService.searchExecutions(type, status, startTime, endTime, resourceId, pageable);
        return success(records);
    }

    @Operation(summary = "Get detailed execution statistics")
    @GetMapping("/detailed-statistics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDetailedStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Map<String, Object>> statistics = executionRecordService.getDetailedStatistics(start, end);
        return success(statistics);
    }

    @Operation(summary = "Check if there's a running execution")
    @GetMapping("/running")
    public ResponseEntity<ApiResponse<Boolean>> hasRunningExecution(
            @RequestParam ExecutionRecord.ExecutionType type,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long workflowId) {
        boolean hasRunning = executionRecordService.hasRunningExecution(type, taskId, workflowId);
        return success(hasRunning);
    }

    @Operation(summary = "Start execution")
    @PostMapping("/start/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> startExecution(@PathVariable String executionId) {
        ExecutionRecord record = executionRecordService.startExecution(executionId);
        return success(record);
    }

    @Operation(summary = "Complete execution")
    @PostMapping("/complete/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> completeExecution(
            @PathVariable String executionId,
            @RequestParam String result) {
        ExecutionRecord record = executionRecordService.completeExecution(executionId, result);
        return success(record);
    }

    @Operation(summary = "Fail execution")
    @PostMapping("/fail/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> failExecution(
            @PathVariable String executionId,
            @RequestParam String errorMessage,
            @RequestParam String stackTrace) {
        ExecutionRecord record = executionRecordService.failExecution(executionId, errorMessage, stackTrace);
        return success(record);
    }

    @Operation(summary = "Retry execution")
    @PostMapping("/retry/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> retryExecution(
            @PathVariable String executionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nextRetryTime) {
        ExecutionRecord record = executionRecordService.retryExecution(executionId, nextRetryTime);
        return success(record);
    }

    @Operation(summary = "Cancel execution")
    @PostMapping("/cancel/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> cancelExecution(
            @PathVariable String executionId,
            @RequestParam String reason) {
        ExecutionRecord record = executionRecordService.cancelExecution(executionId, reason);
        return success(record);
    }

    @Operation(summary = "Mark execution as timed out")
    @PostMapping("/timeout/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionRecord>> timeoutExecution(@PathVariable String executionId) {
        ExecutionRecord record = executionRecordService.timeoutExecution(executionId);
        return success(record);
    }

    @Operation(summary = "Get execution success rate")
    @GetMapping("/success-rate")
    public ResponseEntity<ApiResponse<Double>> getSuccessRate(
            @RequestParam ExecutionRecord.ExecutionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Double successRate = executionRecordService.getSuccessRate(type, start, end);
        return success(successRate);
    }

    @Operation(summary = "Get execution trend data")
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionTrend(
            @RequestParam ExecutionRecord.ExecutionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam String interval) {
        Map<String, Object> trend = executionRecordService.getExecutionTrend(type, start, end, interval);
        return success(trend);
    }

    @Operation(summary = "Get resource execution summary")
    @GetMapping("/resource-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResourceExecutionSummary(
            @RequestParam ExecutionRecord.ExecutionType type,
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Object> summary = executionRecordService.getResourceExecutionSummary(type, resourceId, start, end);
        return success(summary);
    }

    @Operation(summary = "Clean up old execution records")
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Void>> cleanupOldRecords(@RequestParam int retentionDays) {
        executionRecordService.cleanupOldRecords(retentionDays);
        return success();
    }

    @Operation(summary = "Export execution records")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExecutionRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam String format) {
        byte[] data = executionRecordService.exportExecutionRecords(start, end, format);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Get execution metrics for monitoring")
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Object> metrics = executionRecordService.getExecutionMetrics(start, end);
        return success(metrics);
    }
}
