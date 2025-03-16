package com.jobflow.service.impl;

import com.jobflow.dao.OperationLogDao;
import com.jobflow.domain.OperationLog;
import com.jobflow.service.AbstractBaseService;
import com.jobflow.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class OperationLogServiceImpl extends AbstractBaseService<OperationLog> implements OperationLogService {

    private final OperationLogDao operationLogDao;

    @Autowired
    public OperationLogServiceImpl(OperationLogDao operationLogDao) {
        super(operationLogDao, operationLogDao);
        this.operationLogDao = operationLogDao;
    }

    @Override
    protected OperationLog.OperationModule getOperationModule() {
        return OperationLog.OperationModule.SYSTEM;
    }

    @Override
    protected String getEntityName() {
        return "OperationLog";
    }

    @Override
    public List<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Long tenantId) {
        return operationLogDao.findByTimeRange(startTime, endTime, tenantId);
    }

    @Override
    public List<OperationLog> findByOperator(Long operatorId, Long tenantId) {
        return operationLogDao.findByOperator(operatorId, tenantId);
    }

    @Override
    public List<OperationLog> findByModuleAndResource(OperationLog.OperationModule module,
                                                     String resourceId, Long tenantId) {
        return operationLogDao.findByModuleAndResource(module, resourceId, tenantId);
    }

    @Override
    public List<OperationLog> findByType(OperationLog.OperationType type, Long tenantId) {
        return operationLogDao.findByType(type, tenantId);
    }

    @Override
    public List<OperationLog> findByStatus(OperationLog.OperationStatus status, Long tenantId) {
        return operationLogDao.findByStatus(status, tenantId);
    }

    @Override
    public OperationLogStatistics getOperationLogStatistics(Long tenantId) {
        List<OperationLog> logs = findByTenantId(tenantId);
        OperationLogStatistics stats = new OperationLogStatistics();
        
        stats.setTotalOperations(logs.size());
        stats.setSuccessfulOperations(logs.stream()
            .filter(log -> log.getStatus() == OperationLog.OperationStatus.SUCCESS)
            .count());
        stats.setFailedOperations(logs.stream()
            .filter(log -> log.getStatus() == OperationLog.OperationStatus.FAILED)
            .count());

        // Group by type
        stats.setOperationsByType(logs.stream()
            .collect(Collectors.groupingBy(OperationLog::getType, Collectors.counting())));

        // Group by module
        stats.setOperationsByModule(logs.stream()
            .collect(Collectors.groupingBy(OperationLog::getModule, Collectors.counting())));

        // Group by operator
        stats.setOperationsByOperator(logs.stream()
            .collect(Collectors.groupingBy(OperationLog::getOperatorName, Collectors.counting())));

        // Calculate average duration
        double avgDuration = logs.stream()
            .mapToLong(OperationLog::getDuration)
            .average()
            .orElse(0.0);
        stats.setAverageOperationDuration(avgDuration);

        // Calculate success rate
        stats.setSuccessRate(stats.getSuccessfulOperations() * 100.0 / stats.getTotalOperations());

        // Get last operation time
        stats.setLastOperationTime(logs.stream()
            .map(OperationLog::getStartTime)
            .max(LocalDateTime::compareTo)
            .orElse(null));

        return stats;
    }

    @Override
    public OperationTrends getOperationTrends(Long tenantId, int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        List<OperationLog> logs = findByTimeRange(startTime, endTime, tenantId);

        OperationTrends trends = new OperationTrends();
        
        // Generate time points (one per day)
        List<LocalDateTime> timePoints = IntStream.range(0, days)
            .mapToObj(i -> startTime.plusDays(i))
            .collect(Collectors.toList());
        trends.setTimePoints(timePoints);

        // Initialize data structures
        Map<OperationLog.OperationType, List<Long>> operationCounts = new HashMap<>();
        Map<OperationLog.OperationModule, List<Long>> moduleCounts = new HashMap<>();
        List<Double> successRates = new ArrayList<>();
        List<Double> averageDurations = new ArrayList<>();

        // Calculate trends for each day
        for (LocalDateTime day : timePoints) {
            LocalDateTime nextDay = day.plusDays(1);
            List<OperationLog> dayLogs = logs.stream()
                .filter(log -> !log.getStartTime().isBefore(day) && log.getStartTime().isBefore(nextDay))
                .collect(Collectors.toList());

            // Operation counts by type
            for (OperationLog.OperationType type : OperationLog.OperationType.values()) {
                operationCounts.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(dayLogs.stream()
                        .filter(log -> log.getType() == type)
                        .count());
            }

            // Operation counts by module
            for (OperationLog.OperationModule module : OperationLog.OperationModule.values()) {
                moduleCounts.computeIfAbsent(module, k -> new ArrayList<>())
                    .add(dayLogs.stream()
                        .filter(log -> log.getModule() == module)
                        .count());
            }

            // Success rate
            long successful = dayLogs.stream()
                .filter(log -> log.getStatus() == OperationLog.OperationStatus.SUCCESS)
                .count();
            double successRate = dayLogs.isEmpty() ? 0.0 : 
                               (double) successful / dayLogs.size() * 100;
            successRates.add(successRate);

            // Average duration
            double avgDuration = dayLogs.stream()
                .mapToLong(OperationLog::getDuration)
                .average()
                .orElse(0.0);
            averageDurations.add(avgDuration);
        }

        trends.setOperationCounts(operationCounts);
        trends.setModuleCounts(moduleCounts);
        trends.setSuccessRates(successRates);
        trends.setAverageDurations(averageDurations);

        return trends;
    }

    @Override
    public byte[] exportOperationLogs(LocalDateTime startTime, LocalDateTime endTime, Long tenantId) {
        List<OperationLog> logs = findByTimeRange(startTime, endTime, tenantId);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 
                                               CSVFormat.DEFAULT)) {
            
            // Write header
            printer.printRecord(
                "Operation ID",
                "Type",
                "Module",
                "Status",
                "Operator",
                "Resource Type",
                "Resource ID",
                "Operation",
                "Start Time",
                "End Time",
                "Duration (ms)",
                "Client IP",
                "Result"
            );

            // Write data
            for (OperationLog log : logs) {
                printer.printRecord(
                    log.getOperationId(),
                    log.getType(),
                    log.getModule(),
                    log.getStatus(),
                    log.getOperatorName(),
                    log.getResourceType(),
                    log.getResourceId(),
                    log.getOperation(),
                    log.getStartTime(),
                    log.getEndTime(),
                    log.getDuration(),
                    log.getClientIp(),
                    log.getResult()
                );
            }

            printer.flush();
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to export operation logs: {}", e.getMessage());
            throw new RuntimeException("Failed to export operation logs", e);
        }
    }
}
