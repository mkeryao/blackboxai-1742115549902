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
    start_time DATETIME,
    end_time DATETIME,
    workflow_id BIGINT,
    sequence INTEGER,
    parameters TEXT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id)
);

-- Task Notification Configuration table
CREATE TABLE IF NOT EXISTS fj_task_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    notify_on VARCHAR(20) NOT NULL,  -- SUCCESS, FAILURE, START, etc.
    channel VARCHAR(20) NOT NULL,     -- EMAIL, SLACK, WEBHOOK, etc.
    recipient VARCHAR(255) NOT NULL,  -- Email, user ID, webhook URL, etc.
    template TEXT,                    -- Notification template
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (task_id) REFERENCES fj_task(id) ON DELETE CASCADE
);

-- Workflow table
CREATE TABLE IF NOT EXISTS fj_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cron VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    timeout INTEGER,
    retries INTEGER,
    retry_delay INTEGER,
    parameters TEXT,
    concurrent BOOLEAN DEFAULT FALSE,
    error_handling VARCHAR(20),
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME
);

-- Workflow Notification Configuration table
CREATE TABLE IF NOT EXISTS fj_workflow_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    notify_on VARCHAR(20) NOT NULL,  -- SUCCESS, FAILURE, START, etc.
    channel VARCHAR(20) NOT NULL,     -- EMAIL, SLACK, WEBHOOK, etc.
    recipient VARCHAR(255) NOT NULL,  -- Email, user ID, webhook URL, etc.
    template TEXT,                    -- Notification template
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME,
    FOREIGN KEY (workflow_id) REFERENCES fj_workflow(id) ON DELETE CASCADE
);

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

-- Notification History table
CREATE TABLE IF NOT EXISTS fj_notification_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,        -- TASK or WORKFLOW
    source_id BIGINT NOT NULL,        -- Task ID or Workflow ID
    event_type VARCHAR(20) NOT NULL,  -- SUCCESS, FAILURE, etc.
    channel VARCHAR(20) NOT NULL,     -- EMAIL, SLACK, etc.
    recipient VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,      -- SENT, FAILED, etc.
    error_message TEXT,
    tenant_id BIGINT NOT NULL,
    created_by VARCHAR(50),
    created_time DATETIME,
    updated_by VARCHAR(50),
    updated_time DATETIME
);

-- Indexes
CREATE INDEX idx_task_tenant ON fj_task(tenant_id);
CREATE INDEX idx_task_workflow ON fj_task(workflow_id);
CREATE INDEX idx_task_status ON fj_task(status);
CREATE INDEX idx_task_schedule ON fj_task(start_time, end_time);

CREATE INDEX idx_task_notification_task ON fj_task_notification(task_id);
CREATE INDEX idx_task_notification_tenant ON fj_task_notification(tenant_id);

CREATE INDEX idx_workflow_tenant ON fj_workflow(tenant_id);
CREATE INDEX idx_workflow_status ON fj_workflow(status);
CREATE INDEX idx_workflow_schedule ON fj_workflow(start_time, end_time);

CREATE INDEX idx_workflow_notification_workflow ON fj_workflow_notification(workflow_id);
CREATE INDEX idx_workflow_notification_tenant ON fj_workflow_notification(tenant_id);

CREATE INDEX idx_workflow_dependency_workflow ON fj_workflow_dependency(workflow_id);
CREATE INDEX idx_workflow_dependency_dependency ON fj_workflow_dependency(dependency_id);
CREATE INDEX idx_workflow_dependency_tenant ON fj_workflow_dependency(tenant_id);

CREATE INDEX idx_execution_tenant ON fj_execution_record(tenant_id);
CREATE INDEX idx_execution_type ON fj_execution_record(type);
CREATE INDEX idx_execution_status ON fj_execution_record(status);
CREATE INDEX idx_execution_start_time ON fj_execution_record(start_time);
CREATE INDEX idx_execution_task ON fj_execution_record(task_id);
CREATE INDEX idx_execution_workflow ON fj_execution_record(workflow_id);
CREATE INDEX idx_execution_id ON fj_execution_record(execution_id);
CREATE INDEX idx_execution_retry ON fj_execution_record(next_retry_time);

CREATE INDEX idx_notification_history_type ON fj_notification_history(type, source_id);
CREATE INDEX idx_notification_history_tenant ON fj_notification_history(tenant_id);
CREATE INDEX idx_notification_history_time ON fj_notification_history(created_time);
