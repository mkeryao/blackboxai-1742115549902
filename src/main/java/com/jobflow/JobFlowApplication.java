package com.jobflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JobFlow Application Main Class
 * 
 * A distributed task scheduling and workflow management system that provides:
 * - Task scheduling and execution
 * - Workflow management with DAG support
 * - Distributed locking
 * - User management and authentication
 * - Notification system
 * - Operation logging and monitoring
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableTransactionManagement
public class JobFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobFlowApplication.class, args);
    }
}
