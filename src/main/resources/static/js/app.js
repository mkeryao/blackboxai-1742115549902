// JobFlow Application JavaScript
const JobFlow = {
    // API Endpoints
    api: {
        // Task Operations
        async getTasks() {
            return await this._fetch('/api/tasks');
        },
        async createTask(task) {
            return await this._fetch('/api/tasks', 'POST', task);
        },
        async updateTask(task) {
            return await this._fetch(`/api/tasks/${task.id}`, 'PUT', task);
        },
        async deleteTask(taskId) {
            return await this._fetch(`/api/tasks/${taskId}`, 'DELETE');
        },

        // Workflow Operations
        async getWorkflows() {
            return await this._fetch('/api/workflows');
        },
        async createWorkflow(workflow) {
            return await this._fetch('/api/workflows', 'POST', workflow);
        },
        async updateWorkflow(workflow) {
            return await this._fetch(`/api/workflows/${workflow.id}`, 'PUT', workflow);
        },
        async deleteWorkflow(workflowId) {
            return await this._fetch(`/api/workflows/${workflowId}`, 'DELETE');
        },

        // Execution Records Operations
        async getExecutionRecords() {
            return await this._fetch('/api/execution-records');
        },
        async getExecutionMetrics() {
            return await this._fetch('/api/execution-records/metrics');
        },
        async exportExecutionRecords() {
            return await this._fetch('/api/execution-records/export');
        },

        // Calendar Operations
        async getCalendarEvents() {
            return await this._fetch('/api/calendar/events');
        },
        async createEvent(event) {
            return await this._fetch('/api/calendar/events', 'POST', event);
        },
        async updateEvent(event) {
            return await this._fetch(`/api/calendar/events/${event.id}`, 'PUT', event);
        },
        async deleteEvent(eventId) {
            return await this._fetch(`/api/calendar/events/${eventId}`, 'DELETE');
        },

        // Dashboard Operations
        async getDashboardData() {
            return await this._fetch('/api/dashboard');
        },

        // Helper method for making API calls
        async _fetch(url, method = 'GET', body = null) {
            try {
                const options = {
                    method,
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${this.getToken()}`
                    }
                };

                if (body) {
                    options.body = JSON.stringify(body);
                }

                const response = await fetch(url, options);
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return await response.json();
                }
                
                return await response.text();
            } catch (error) {
                console.error('API Error:', error);
                throw error;
            }
        },

        // Token management
        getToken() {
            return localStorage.getItem('token');
        },
        setToken(token) {
            localStorage.setItem('token', token);
        },
        removeToken() {
            localStorage.removeItem('token');
        }
    },

    // UI Utilities
    UI: {
        // Show loading spinner
        showLoading() {
            const loader = document.createElement('div');
            loader.className = 'fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50';
            loader.innerHTML = `
                <div class="bg-white p-4 rounded-lg flex items-center">
                    <i class="fas fa-spinner fa-spin text-indigo-600 mr-3"></i>
                    <span class="text-gray-700">Loading...</span>
                </div>
            `;
            document.body.appendChild(loader);
            return loader;
        },

        // Hide loading spinner
        hideLoading(loader) {
            if (loader && loader.parentNode) {
                loader.parentNode.removeChild(loader);
            }
        },

        // Show notification
        showNotification(message, type = 'info') {
            const notification = document.createElement('div');
            notification.className = `notification notification-${type}`;
            notification.innerHTML = `
                <div class="flex items-center">
                    <i class="fas ${this._getNotificationIcon(type)} mr-3"></i>
                    <span>${message}</span>
                </div>
            `;
            document.body.appendChild(notification);

            setTimeout(() => {
                notification.remove();
            }, 3000);
        },

        // Get notification icon based on type
        _getNotificationIcon(type) {
            const icons = {
                success: 'fa-check-circle',
                error: 'fa-exclamation-circle',
                warning: 'fa-exclamation-triangle',
                info: 'fa-info-circle'
            };
            return icons[type] || icons.info;
        },

        // Format date
        formatDate(date) {
            return new Date(date).toLocaleString();
        },

        // Format duration
        formatDuration(duration) {
            if (!duration) return 'N/A';
            
            const seconds = Math.floor(duration / 1000);
            if (seconds < 60) return `${seconds}s`;
            
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = seconds % 60;
            if (minutes < 60) return `${minutes}m ${remainingSeconds}s`;
            
            const hours = Math.floor(minutes / 60);
            const remainingMinutes = minutes % 60;
            return `${hours}h ${remainingMinutes}m ${remainingSeconds}s`;
        }
    },

    // Authentication
    Auth: {
        // Check if user is authenticated
        isAuthenticated() {
            return !!JobFlow.api.getToken();
        },

        // Login
        async login(username, password) {
            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ username, password })
                });

                if (!response.ok) {
                    throw new Error('Login failed');
                }

                const data = await response.json();
                JobFlow.api.setToken(data.token);
                return true;
            } catch (error) {
                console.error('Login Error:', error);
                throw error;
            }
        },

        // Logout
        logout() {
            JobFlow.api.removeToken();
            window.location.href = '/login.html';
        }
    }
};

// Check authentication on page load
document.addEventListener('DOMContentLoaded', () => {
    const publicPages = ['/login.html', '/register.html', '/forgot-password.html'];
    const isPublicPage = publicPages.some(page => window.location.pathname.endsWith(page));

    if (!isPublicPage && !JobFlow.Auth.isAuthenticated()) {
        window.location.href = '/login.html';
    }
});
