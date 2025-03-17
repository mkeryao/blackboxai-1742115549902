-- Task table
CREATE TABLE IF NOT EXISTS fj_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    command TEXT NOT NULL,
    cron VARCHAR(100),
    timeout INTEGER,
    retries INTEGER,
    retry_delay INTEGER,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    start_time DATETIME,           -- Added start time
    end_time DATETIME,            -- Added end time
    workflow_id BIGINT,
    sequence INTEGER,
    parameters TEXT,
    notification TEXT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id)
);

-- Task indexes
CREATE INDEX idx_task_tenant ON fj_task(tenant_id);
CREATE INDEX idx_task_workflow ON fj_task(workflow_id);
CREATE INDEX idx_task_status ON fj_task(status);
CREATE INDEX idx_task_schedule ON fj_task(start_time, end_time);

-- Workflow table
CREATE TABLE IF NOT EXISTS fj_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cron VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    start_time DATETIME,           -- Added start time
    end_time DATETIME,            -- Added end time
    timeout INTEGER,
    retries INTEGER,
    retry_delay INTEGER,
    notification TEXT,
    parameters TEXT,
    concurrent BOOLEAN DEFAULT FALSE,
    error_handling VARCHAR(20),
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME
);

-- Workflow indexes
CREATE INDEX idx_workflow_tenant ON fj_workflow(tenant_id);
CREATE INDEX idx_workflow_status ON fj_workflow(status);
CREATE INDEX idx_workflow_schedule ON fj_workflow(start_time, end_time);

-- Workflow Dependencies table
CREATE TABLE IF NOT EXISTS fj_workflow_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    dependency_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id),
    FOREIGN KEY (dependency_id) REFERENCES fj_workflow(id)
);

-- Workflow Dependencies indexes
CREATE INDEX idx_dependency_workflow ON fj_workflow_dependency(workflow_id);
CREATE INDEX idx_dependency_dependency ON fj_workflow_dependency(dependency_id);
CREATE INDEX idx_dependency_tenant ON fj_workflow_dependency(tenant_id);

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

-- Calendar indexes
CREATE INDEX idx_calendar_tenant ON fj_calendar(tenant_id);
CREATE INDEX idx_calendar_type ON fj_calendar(event_type);
CREATE INDEX idx_calendar_status ON fj_calendar(status);
CREATE INDEX idx_calendar_time ON fj_calendar(start_time, end_time);
CREATE INDEX idx_calendar_task ON fj_calendar(task_id);
CREATE INDEX idx_calendar_workflow ON fj_calendar(workflow_id);
