package com.jobflow.service.impl;

import com.jobflow.dao.WorkflowDao;
import com.jobflow.dao.WorkflowDependencyDao;
import com.jobflow.dao.TaskDao;
import com.jobflow.dao.OperationLogDao;
import com.jobflow.dao.NotificationDao;
import com.jobflow.domain.*;
import com.jobflow.lock.DistributedLock;
import com.jobflow.service.AbstractBaseService;
import com.jobflow.service.TaskService;
import com.jobflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowServiceImpl extends AbstractBaseService<Workflow> implements WorkflowService {

    private final WorkflowDao workflowDao;
    private final WorkflowDependencyDao dependencyDao;
    private final TaskDao taskDao;
    private final TaskService taskService;
    private final NotificationDao notificationDao;
    private final DistributedLock distributedLock;

    // Cache for storing running workflow states
    private final Map<Long, WorkflowExecutionState> executionStates = new ConcurrentHashMap<>();

    @Autowired
    public WorkflowServiceImpl(WorkflowDao workflowDao,
                             WorkflowDependencyDao dependencyDao,
                             TaskDao taskDao,
                             TaskService taskService,
                             OperationLogDao operationLogDao,
                             NotificationDao notificationDao,
                             DistributedLock distributedLock) {
        super(workflowDao, operationLogDao);
        this.workflowDao = workflowDao;
        this.dependencyDao = dependencyDao;
        this.taskDao = taskDao;
        this.taskService = taskService;
        this.notificationDao = notificationDao;
        this.distributedLock = distributedLock;
    }

    @Override
    protected OperationLog.OperationModule getOperationModule() {
        return OperationLog.OperationModule.WORKFLOW;
    }

    @Override
    protected String getEntityName() {
        return "Workflow";
    }

    @Override
    public List<Workflow> findDueWorkflows(Long tenantId) {
        return workflowDao.findDueWorkflows(tenantId);
    }

    @Override
    @Transactional
    public void executeWorkflow(Workflow workflow, String operator) {
        String lockKey = "workflow_execution_" + workflow.getId();
        
        try {
            if (!distributedLock.acquire(lockKey)) {
                log.warn("Failed to acquire lock for workflow: {}", workflow.getId());
                return;
            }

            workflow.markAsRunning();
            workflowDao.update(workflow, operator);

            List<Task> sortedTasks = getTopologicalSort(workflow.getId());
            if (sortedTasks.isEmpty()) {
                log.warn("No tasks found in workflow: {}", workflow.getId());
                return;
            }

            WorkflowExecutionState state = new WorkflowExecutionState(workflow, sortedTasks);
            executionStates.put(workflow.getId(), state);

            // Start execution with root tasks (tasks with no dependencies)
            List<Task> rootTasks = findRootTasks(workflow.getId(), sortedTasks);
            for (Task task : rootTasks) {
                executeWorkflowTask(workflow.getId(), task, operator);
            }

        } finally {
            distributedLock.release(lockKey);
        }
    }

    @Override
    @Transactional
    public void executeWorkflowFromTask(Long workflowId, Long taskId, String operator) {
        Workflow workflow = findById(workflowId);
        Task startTask = taskDao.findById(taskId);
        
        if (workflow == null || startTask == null) {
            throw new IllegalArgumentException("Workflow or task not found");
        }

        List<Task> downstreamTasks = getDownstreamTasks(workflowId, taskId);
        downstreamTasks.add(0, startTask);

        workflow.markAsRunning();
        workflowDao.update(workflow, operator);

        WorkflowExecutionState state = new WorkflowExecutionState(workflow, downstreamTasks);
        executionStates.put(workflowId, state);

        executeWorkflowTask(workflowId, startTask, operator);
    }

    private void executeWorkflowTask(Long workflowId, Task task, String operator) {
        WorkflowExecutionState state = executionStates.get(workflowId);
        if (state == null || !canExecuteTask(state, task)) {
            return;
        }

        state.markTaskAsRunning(task.getId());
        taskService.executeTask(task, operator);
    }

    private boolean canExecuteTask(WorkflowExecutionState state, Task task) {
        List<WorkflowDependency> dependencies = dependencyDao.findByTargetTaskId(
            state.getWorkflow().getId(), task.getId());

        for (WorkflowDependency dependency : dependencies) {
            Task sourceTask = taskDao.findById(dependency.getSourceTaskId());
            if (!dependency.isSatisfied(sourceTask)) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional
    public void cancelWorkflow(Long workflowId, String operator) {
        Workflow workflow = findById(workflowId);
        if (workflow == null || workflow.getStatus() != Workflow.WorkflowStatus.RUNNING) {
            throw new IllegalStateException("Workflow is not running");
        }

        workflow.setStatus(Workflow.WorkflowStatus.CANCELLED);
        workflowDao.update(workflow, operator);

        // Cancel all running tasks
        WorkflowExecutionState state = executionStates.get(workflowId);
        if (state != null) {
            state.getRunningTasks().forEach(taskId -> 
                taskService.cancelTask(taskId, operator));
            executionStates.remove(workflowId);
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long workflowId, Workflow.WorkflowStatus status, String operator) {
        workflowDao.updateStatus(workflowId, status, operator);
    }

    @Override
    public Workflow findByName(String name, Long tenantId) {
        return workflowDao.findByName(name, tenantId);
    }

    @Override
    @Transactional
    public void addTask(Long workflowId, Task task, String operator) {
        Workflow workflow = findById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found");
        }

        task.setTenantId(workflow.getTenantId());
        Long taskId = taskDao.insert(task, operator);
        task.setId(taskId);

        // Update workflow task count
        workflow.setTotalTasks(workflow.getTotalTasks() + 1);
        workflowDao.update(workflow, operator);
    }

    @Override
    @Transactional
    public void removeTask(Long workflowId, Long taskId, String operator) {
        // Remove all dependencies first
        List<WorkflowDependency> dependencies = dependencyDao.findByWorkflowId(workflowId);
        dependencies.stream()
            .filter(d -> d.getSourceTaskId().equals(taskId) || d.getTargetTaskId().equals(taskId))
            .forEach(d -> dependencyDao.delete(d.getId(), operator));

        // Then remove the task
        taskDao.delete(taskId, operator);

        // Update workflow task count
        Workflow workflow = findById(workflowId);
        workflow.setTotalTasks(workflow.getTotalTasks() - 1);
        workflowDao.update(workflow, operator);
    }

    @Override
    @Transactional
    public void addDependency(Long workflowId, Long sourceTaskId, Long targetTaskId,
                            WorkflowDependency.DependencyType type, String operator) {
        // Validate tasks exist
        if (!taskDao.exists(sourceTaskId) || !taskDao.exists(targetTaskId)) {
            throw new IllegalArgumentException("Source or target task not found");
        }

        // Check for existing dependency
        if (dependencyDao.existsDependency(workflowId, sourceTaskId, targetTaskId)) {
            throw new IllegalStateException("Dependency already exists");
        }

        // Create new dependency
        WorkflowDependency dependency = new WorkflowDependency();
        dependency.setWorkflowId(workflowId);
        dependency.setSourceTaskId(sourceTaskId);
        dependency.setTargetTaskId(targetTaskId);
        dependency.setType(type);
        dependency.setTenantId(findById(workflowId).getTenantId());

        dependencyDao.insert(dependency, operator);

        // Validate no cycles were created
        if (!validateWorkflowDag(workflowId)) {
            dependencyDao.delete(dependency.getId(), operator);
            throw new IllegalStateException("Adding this dependency would create a cycle");
        }
    }

    @Override
    @Transactional
    public void removeDependency(Long workflowId, Long sourceTaskId, Long targetTaskId, 
                               String operator) {
        List<WorkflowDependency> dependencies = dependencyDao.findByWorkflowId(workflowId);
        dependencies.stream()
            .filter(d -> d.getSourceTaskId().equals(sourceTaskId) && 
                        d.getTargetTaskId().equals(targetTaskId))
            .forEach(d -> dependencyDao.delete(d.getId(), operator));
    }

    @Override
    public List<Task> getWorkflowTasks(Long workflowId) {
        // TODO: Implement method to get all tasks in workflow
        return new ArrayList<>();
    }

    @Override
    public List<WorkflowDependency> getWorkflowDependencies(Long workflowId) {
        return dependencyDao.findByWorkflowId(workflowId);
    }

    @Override
    public boolean validateWorkflowDag(Long workflowId) {
        List<Task> tasks = getWorkflowTasks(workflowId);
        List<WorkflowDependency> dependencies = getWorkflowDependencies(workflowId);
        
        // Build adjacency list
        Map<Long, Set<Long>> graph = new HashMap<>();
        for (Task task : tasks) {
            graph.put(task.getId(), new HashSet<>());
        }
        
        for (WorkflowDependency dep : dependencies) {
            graph.get(dep.getSourceTaskId()).add(dep.getTargetTaskId());
        }

        // Check for cycles using DFS
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        for (Task task : tasks) {
            if (hasCycle(task.getId(), graph, visited, recursionStack)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasCycle(Long taskId, Map<Long, Set<Long>> graph, 
                           Set<Long> visited, Set<Long> recursionStack) {
        if (recursionStack.contains(taskId)) {
            return true;
        }

        if (visited.contains(taskId)) {
            return false;
        }

        visited.add(taskId);
        recursionStack.add(taskId);

        for (Long neighbor : graph.get(taskId)) {
            if (hasCycle(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(taskId);
        return false;
    }

    @Override
    public List<Task> getTopologicalSort(Long workflowId) {
        List<Task> tasks = getWorkflowTasks(workflowId);
        List<WorkflowDependency> dependencies = getWorkflowDependencies(workflowId);
        
        // Build adjacency list and in-degree count
        Map<Long, Set<Long>> graph = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        
        for (Task task : tasks) {
            graph.put(task.getId(), new HashSet<>());
            inDegree.put(task.getId(), 0);
        }
        
        for (WorkflowDependency dep : dependencies) {
            graph.get(dep.getSourceTaskId()).add(dep.getTargetTaskId());
            inDegree.merge(dep.getTargetTaskId(), 1, Integer::sum);
        }

        // Perform topological sort using Kahn's algorithm
        Queue<Long> queue = new LinkedList<>();
        inDegree.forEach((taskId, degree) -> {
            if (degree == 0) queue.offer(taskId);
        });

        List<Task> result = new ArrayList<>();
        Map<Long, Task> taskMap = tasks.stream()
            .collect(Collectors.toMap(Task::getId, task -> task));

        while (!queue.isEmpty()) {
            Long taskId = queue.poll();
            result.add(taskMap.get(taskId));

            for (Long neighbor : graph.get(taskId)) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return result.size() == tasks.size() ? result : new ArrayList<>();
    }

    private List<Task> findRootTasks(Long workflowId, List<Task> tasks) {
        Set<Long> targetTasks = dependencyDao.findByWorkflowId(workflowId).stream()
            .map(WorkflowDependency::getTargetTaskId)
            .collect(Collectors.toSet());

        return tasks.stream()
            .filter(task -> !targetTasks.contains(task.getId()))
            .collect(Collectors.toList());
    }

    private List<Task> getDownstreamTasks(Long workflowId, Long taskId) {
        Set<Long> visited = new HashSet<>();
        List<Task> result = new ArrayList<>();
        collectDownstreamTasks(workflowId, taskId, visited, result);
        return result;
    }

    private void collectDownstreamTasks(Long workflowId, Long taskId, 
                                      Set<Long> visited, List<Task> result) {
        if (!visited.add(taskId)) {
            return;
        }

        Task task = taskDao.findById(taskId);
        if (task != null) {
            result.add(task);
        }

        List<WorkflowDependency> dependencies = dependencyDao.findBySourceTaskId(workflowId, taskId);
        for (WorkflowDependency dep : dependencies) {
            collectDownstreamTasks(workflowId, dep.getTargetTaskId(), visited, result);
        }
    }

    @Override
    public List<Workflow> getExecutionHistory(Long workflowId, Long tenantId) {
        // TODO: Implement execution history retrieval
        return new ArrayList<>();
    }

    @Override
    public WorkflowStatistics getWorkflowStatistics(Long tenantId) {
        // TODO: Implement workflow statistics calculation
        return new WorkflowStatistics();
    }

    @Override
    public Map<String, List<Task>> getTaskDependencies(Long workflowId, Long taskId) {
        Map<String, List<Task>> result = new HashMap<>();
        
        // Get upstream tasks
        List<Task> upstream = new ArrayList<>();
        List<WorkflowDependency> upstreamDeps = dependencyDao.findByTargetTaskId(workflowId, taskId);
        for (WorkflowDependency dep : upstreamDeps) {
            Task task = taskDao.findById(dep.getSourceTaskId());
            if (task != null) {
                upstream.add(task);
            }
        }
        
        // Get downstream tasks
        List<Task> downstream = new ArrayList<>();
        List<WorkflowDependency> downstreamDeps = dependencyDao.findBySourceTaskId(workflowId, taskId);
        for (WorkflowDependency dep : downstreamDeps) {
            Task task = taskDao.findById(dep.getTargetTaskId());
            if (task != null) {
                downstream.add(task);
            }
        }

        result.put("upstream", upstream);
        result.put("downstream", downstream);
        return result;
    }

    @Override
    public double getWorkflowProgress(Long workflowId) {
        Workflow workflow = findById(workflowId);
        if (workflow == null) {
            return 0.0;
        }
        return workflow.getProgress();
    }

    @Override
    public LocalDateTime getEstimatedCompletionTime(Long workflowId) {
        // TODO: Implement completion time estimation based on historical execution times
        return LocalDateTime.now().plusHours(1);
    }

    /**
     * Inner class to track workflow execution state
     */
    private static class WorkflowExecutionState {
        private final Workflow workflow;
        private final List<Task> tasks;
        private final Set<Long> completedTasks;
        private final Set<Long> runningTasks;
        private final LocalDateTime startTime;

        public WorkflowExecutionState(Workflow workflow, List<Task> tasks) {
            this.workflow = workflow;
            this.tasks = tasks;
            this.completedTasks = new HashSet<>();
            this.runningTasks = new HashSet<>();
            this.startTime = LocalDateTime.now();
        }

        public Workflow getWorkflow() {
            return workflow;
        }

        public List<Task> getTasks() {
            return tasks;
        }

        public Set<Long> getRunningTasks() {
            return runningTasks;
        }

        public void markTaskAsRunning(Long taskId) {
            runningTasks.add(taskId);
        }

        public void markTaskAsCompleted(Long taskId) {
            runningTasks.remove(taskId);
            completedTasks.add(taskId);
        }

        public boolean isCompleted() {
            return completedTasks.size() == tasks.size();
        }

        public double getProgress() {
            return tasks.isEmpty() ? 0.0 : 
                   (double) completedTasks.size() / tasks.size() * 100;
        }
    }
}
