// JobFlow Frontend Application

// Constants
const API_BASE_URL = '/api';
const TOKEN_KEY = 'jobflow_token';

// API Service
const api = {
    // Authentication
    async login(username, password) {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await response.json();
        if (response.ok) {
            localStorage.setItem(TOKEN_KEY, data.token);
            return data;
        }
        throw new Error(data.message);
    },

    // Tasks
    async getTasks() {
        return await this.authenticatedRequest(`${API_BASE_URL}/tasks`);
    },

    async createTask(task) {
        return await this.authenticatedRequest(`${API_BASE_URL}/tasks`, {
            method: 'POST',
            body: JSON.stringify(task)
        });
    },

    async updateTask(taskId, task) {
        return await this.authenticatedRequest(`${API_BASE_URL}/tasks/${taskId}`, {
            method: 'PUT',
            body: JSON.stringify(task)
        });
    },

    async deleteTask(taskId) {
        return await this.authenticatedRequest(`${API_BASE_URL}/tasks/${taskId}`, {
            method: 'DELETE'
        });
    },

    // Workflows
    async getWorkflows() {
        return await this.authenticatedRequest(`${API_BASE_URL}/workflows`);
    },

    async createWorkflow(workflow) {
        return await this.authenticatedRequest(`${API_BASE_URL}/workflows`, {
            method: 'POST',
            body: JSON.stringify(workflow)
        });
    },

    // Helper method for authenticated requests
    async authenticatedRequest(url, options = {}) {
        const token = localStorage.getItem(TOKEN_KEY);
        if (!token) {
            throw new Error('Not authenticated');
        }

        const response = await fetch(url, {
            ...options,
            headers: {
                ...options.headers,
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message);
        }
        return data;
    }
};

// UI Components
const UI = {
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification ${type} animate-fade-in`;
        notification.innerHTML = `
            <div class="flex items-center">
                <div class="flex-shrink-0">
                    ${this.getNotificationIcon(type)}
                </div>
                <div class="ml-3">
                    <p class="text-sm font-medium text-gray-900">${message}</p>
                </div>
                <div class="ml-auto pl-3">
                    <button class="close-notification">×</button>
                </div>
            </div>
        `;

        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 5000);
    },

    getNotificationIcon(type) {
        const icons = {
            success: '<i class="fas fa-check-circle text-green-500"></i>',
            error: '<i class="fas fa-exclamation-circle text-red-500"></i>',
            warning: '<i class="fas fa-exclamation-triangle text-yellow-500"></i>',
            info: '<i class="fas fa-info-circle text-blue-500"></i>'
        };
        return icons[type] || icons.info;
    },

    showLoading() {
        const loader = document.createElement('div');
        loader.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        loader.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(loader);
        return loader;
    },

    hideLoading(loader) {
        loader.remove();
    },

    showModal(title, content) {
        const modal = document.createElement('div');
        modal.className = 'modal-backdrop flex items-center justify-center';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="px-4 py-3 border-b border-gray-200">
                    <h3 class="text-lg font-medium text-gray-900">${title}</h3>
                </div>
                <div class="px-4 py-3">
                    ${content}
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        return modal;
    }
};

// Event Handlers
document.addEventListener('DOMContentLoaded', () => {
    // User menu toggle
    const userMenuButton = document.getElementById('user-menu-button');
    if (userMenuButton) {
        userMenuButton.addEventListener('click', () => {
            // Toggle user menu
        });
    }

    // Initialize notifications
    document.addEventListener('click', (e) => {
        if (e.target.matches('.close-notification')) {
            e.target.closest('.notification').remove();
        }
    });

    // Initialize task actions
    document.addEventListener('click', async (e) => {
        if (e.target.matches('[data-task-action]')) {
            const action = e.target.dataset.taskAction;
            const taskId = e.target.dataset.taskId;
            
            try {
                const loader = UI.showLoading();
                switch (action) {
                    case 'execute':
                        await api.executeTask(taskId);
                        UI.showNotification('Task executed successfully', 'success');
                        break;
                    case 'delete':
                        if (confirm('Are you sure you want to delete this task?')) {
                            await api.deleteTask(taskId);
                            UI.showNotification('Task deleted successfully', 'success');
                            // Refresh task list
                        }
                        break;
                }
                UI.hideLoading(loader);
            } catch (error) {
                UI.showNotification(error.message, 'error');
            }
        }
    });
});

// Dashboard Charts
const Charts = {
    initTaskStatusChart(containerId, data) {
        // Implementation for task status chart
    },

    initWorkflowProgressChart(containerId, data) {
        // Implementation for workflow progress chart
    }
};

// Workflow Visualization
const WorkflowVisualizer = {
    init(containerId, workflowData) {
        // Implementation for workflow visualization
    },

    updateNodeStatus(nodeId, status) {
        // Update node status in visualization
    }
};

// Form Validation
const Validator = {
    validateTask(task) {
        const errors = {};
        if (!task.name) errors.name = 'Task name is required';
        if (!task.type) errors.type = 'Task type is required';
        return errors;
    },

    validateWorkflow(workflow) {
        const errors = {};
        if (!workflow.name) errors.name = 'Workflow name is required';
        if (!workflow.tasks || workflow.tasks.length === 0) {
            errors.tasks = 'Workflow must contain at least one task';
        }
        return errors;
    }
};

// Export modules
window.JobFlow = {
    api,
    UI,
    Charts,
    WorkflowVisualizer,
    Validator
};
