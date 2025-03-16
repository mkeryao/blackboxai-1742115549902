-- Create database
CREATE DATABASE IF NOT EXISTS job_flow;
USE job_flow;

-- Users table
CREATE TABLE IF NOT EXISTS fj_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    wechat_id VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    login_fail_count INT DEFAULT 0,
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    lock_time DATETIME,
    email_notification BOOLEAN DEFAULT TRUE,
    wechat_notification BOOLEAN DEFAULT TRUE,
    language VARCHAR(10) DEFAULT 'en_US',
    preferences TEXT,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    UNIQUE KEY uk_username_tenant (username, tenant_id)
);

-- Tasks table
CREATE TABLE IF NOT EXISTS fj_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    group_name VARCHAR(50),
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    cron_expression VARCHAR(100),
    timeout BIGINT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    retry_interval BIGINT DEFAULT 300000,
    status VARCHAR(20) NOT NULL,
    last_execution_time DATETIME,
    next_execution_time DATETIME,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME
);

-- Workflows table
CREATE TABLE IF NOT EXISTS fj_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    timeout BIGINT,
    concurrent_tasks INT DEFAULT 1,
    last_execution_time DATETIME,
    next_execution_time DATETIME,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME
);

-- Workflow Dependencies table
CREATE TABLE IF NOT EXISTS fj_workflow_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    source_task_id BIGINT NOT NULL,
    target_task_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id) ON DELETE CASCADE,
    FOREIGN KEY (source_task_id) REFERENCES fj_task(id) ON DELETE CASCADE,
    FOREIGN KEY (target_task_id) REFERENCES fj_task(id) ON DELETE CASCADE
);

-- Notifications table
CREATE TABLE IF NOT EXISTS fj_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    level VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    source_id BIGINT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    scheduled_time DATETIME NOT NULL,
    sent_time DATETIME,
    error_message TEXT,
    user_id BIGINT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES fj_user(id) ON DELETE SET NULL
);

-- Operation Logs table
CREATE TABLE IF NOT EXISTS fj_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    module VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    operator_id BIGINT,
    operator_name VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(50),
    operation VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration BIGINT,
    client_ip VARCHAR(50),
    result TEXT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (operator_id) REFERENCES fj_user(id) ON DELETE SET NULL
);

-- Calendar table
CREATE TABLE IF NOT EXISTS fj_calendar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(20) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    all_day BOOLEAN DEFAULT FALSE,
    recurrence_type VARCHAR(20),
    recurrence_interval INTEGER,
    recurrence_end_date DATETIME,
    color VARCHAR(20),
    location VARCHAR(255),
    reminder_minutes INTEGER,
    status VARCHAR(20) NOT NULL,
    task_id BIGINT,
    workflow_id BIGINT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (task_id) REFERENCES fj_task(id) ON DELETE SET NULL,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id) ON DELETE SET NULL
);

-- Execution Records table
CREATE TABLE IF NOT EXISTS fj_execution_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    task_id BIGINT,
    workflow_id BIGINT,
    status VARCHAR(20) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration BIGINT,
    error_message TEXT,
    stack_trace TEXT,
    input_params TEXT,
    output_result TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER,
    next_retry_time DATETIME,
    executor VARCHAR(50),
    executor_ip VARCHAR(50),
    trigger_type VARCHAR(20),
    trigger_info TEXT,
    environment VARCHAR(50),
    resource_usage TEXT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (task_id) REFERENCES fj_task(id) ON DELETE SET NULL,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id) ON DELETE SET NULL
);

-- Execution Records indexes
CREATE INDEX idx_execution_tenant ON fj_execution_record(tenant_id);
CREATE INDEX idx_execution_type ON fj_execution_record(type);
CREATE INDEX idx_execution_status ON fj_execution_record(status);
CREATE INDEX idx_execution_start_time ON fj_execution_record(start_time);
CREATE INDEX idx_execution_task ON fj_execution_record(task_id);
CREATE INDEX idx_execution_workflow ON fj_execution_record(workflow_id);
CREATE INDEX idx_execution_id ON fj_execution_record(execution_id);
CREATE INDEX idx_execution_retry ON fj_execution_record(next_retry_time);

-- Calendar indexes
CREATE INDEX idx_calendar_tenant ON fj_calendar(tenant_id);
CREATE INDEX idx_calendar_type ON fj_calendar(event_type);
CREATE INDEX idx_calendar_status ON fj_calendar(status);
CREATE INDEX idx_calendar_start_time ON fj_calendar(start_time);
CREATE INDEX idx_calendar_end_time ON fj_calendar(end_time);
CREATE INDEX idx_calendar_task ON fj_calendar(task_id);
CREATE INDEX idx_calendar_workflow ON fj_calendar(workflow_id);

-- Insert default admin user
INSERT INTO fj_user (
    username, 
    password, 
    email, 
    status, 
    roles, 
    tenant_id, 
    created_by, 
    created_time, 
    updated_by, 
    updated_time
) VALUES (
    'admin',
    '$2a$10$rS.F0oHaQtMnFEMYa0dR4eGzE.OHCgRarC3HqJDxvuKvOjZJVhKhq', -- admin123
    'admin@jobflow.com',
    'ACTIVE',
    'ROLE_ADMIN',
    0,
    'system',
    NOW(),
    'system',
    NOW()
) ON DUPLICATE KEY UPDATE updated_time = NOW();

-- Create indexes
CREATE INDEX idx_task_tenant ON fj_task(tenant_id);
CREATE INDEX idx_task_group ON fj_task(group_name);
CREATE INDEX idx_task_status ON fj_task(status);
CREATE INDEX idx_task_next_execution ON fj_task(next_execution_time);

CREATE INDEX idx_workflow_tenant ON fj_workflow(tenant_id);
CREATE INDEX idx_workflow_status ON fj_workflow(status);
CREATE INDEX idx_workflow_next_execution ON fj_workflow(next_execution_time);

CREATE INDEX idx_notification_tenant ON fj_notification(tenant_id);
CREATE INDEX idx_notification_user ON fj_notification(user_id);
CREATE INDEX idx_notification_status ON fj_notification(status);
CREATE INDEX idx_notification_scheduled ON fj_notification(scheduled_time);

CREATE INDEX idx_operation_log_tenant ON fj_operation_log(tenant_id);
CREATE INDEX idx_operation_log_operator ON fj_operation_log(operator_id);
CREATE INDEX idx_operation_log_type ON fj_operation_log(type);
CREATE INDEX idx_operation_log_module ON fj_operation_log(module);
CREATE INDEX idx_operation_log_start_time ON fj_operation_log(start_time);
