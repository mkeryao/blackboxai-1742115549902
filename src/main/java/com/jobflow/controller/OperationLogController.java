package com.jobflow.controller;

import com.jobflow.domain.OperationLog;
import com.jobflow.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Operation Log Controller
 * 
 * Handles operation log related endpoints including querying and exporting logs.
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@Tag(name = "Operation Log Management", description = "APIs for managing operation logs")
public class OperationLogController extends BaseController {

    private final OperationLogService operationLogService;

    @Autowired
    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "Get logs by time range")
    @GetMapping("/time-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<OperationLog> logs = operationLogService.findByTimeRange(
                startTime, endTime, getCurrentTenantId());
            return success(logs);
        } catch (Exception e) {
            log.error("Failed to get logs by time range", e);
            return error("Failed to get logs by time range: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get logs by operator")
    @GetMapping("/operator/{operatorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getLogsByOperator(
            @PathVariable Long operatorId) {
        try {
            List<OperationLog> logs = operationLogService.findByOperator(operatorId, getCurrentTenantId());
            return success(logs);
        } catch (Exception e) {
            log.error("Failed to get logs by operator", e);
            return error("Failed to get logs by operator: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get logs by module and resource")
    @GetMapping("/module/{module}/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getLogsByModuleAndResource(
            @PathVariable OperationLog.OperationModule module,
            @PathVariable String resourceId) {
        try {
            List<OperationLog> logs = operationLogService.findByModuleAndResource(
                module, resourceId, getCurrentTenantId());
            return success(logs);
        } catch (Exception e) {
            log.error("Failed to get logs by module and resource", e);
            return error("Failed to get logs by module and resource: " + e.getMessage(), 
                       HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get logs by operation type")
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getLogsByType(
            @PathVariable OperationLog.OperationType type) {
        try {
            List<OperationLog> logs = operationLogService.findByType(type, getCurrentTenantId());
            return success(logs);
        } catch (Exception e) {
            log.error("Failed to get logs by type", e);
            return error("Failed to get logs by type: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get logs by status")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getLogsByStatus(
            @PathVariable OperationLog.OperationStatus status) {
        try {
            List<OperationLog> logs = operationLogService.findByStatus(status, getCurrentTenantId());
            return success(logs);
        } catch (Exception e) {
            log.error("Failed to get logs by status", e);
            return error("Failed to get logs by status: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get operation log statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OperationLogService.OperationLogStatistics>> getOperationLogStatistics() {
        try {
            OperationLogService.OperationLogStatistics statistics = 
                operationLogService.getOperationLogStatistics(getCurrentTenantId());
            return success(statistics);
        } catch (Exception e) {
            log.error("Failed to get operation log statistics", e);
            return error("Failed to get operation log statistics: " + e.getMessage(), 
                       HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get operation trends")
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OperationLogService.OperationTrends>> getOperationTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            OperationLogService.OperationTrends trends = 
                operationLogService.getOperationTrends(getCurrentTenantId(), days);
            return success(trends);
        } catch (Exception e) {
            log.error("Failed to get operation trends", e);
            return error("Failed to get operation trends: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Export operation logs")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOperationLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            byte[] content = operationLogService.exportOperationLogs(
                startTime, endTime, getCurrentTenantId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "operation_logs.csv");

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export operation logs", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
