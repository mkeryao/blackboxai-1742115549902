# JobFlow

JobFlow is a distributed task scheduling and workflow management system built with Spring Boot.

## Features

- Task Scheduling and Execution
  - Support for HTTP, Shell, and Spring Bean tasks
  - Cron-based scheduling
  - Retry mechanism
  - Timeout handling
  - Task grouping

- Workflow Management
  - DAG (Directed Acyclic Graph) support
  - Task dependencies
  - Parallel execution
  - Progress tracking
  - Workflow templates

- User Management
  - Role-based access control
  - Multi-tenant support
  - Authentication and authorization
  - User activity tracking

- Notification System
  - Email notifications
  - WeChat integration
  - Webhook support
  - Customizable templates
  - Retry mechanism

- Operation Logging
  - Comprehensive audit trail
  - Operation statistics
  - Export functionality
  - Trend analysis

- Distributed Architecture
  - Distributed locking
  - Redis-based caching
  - Database-based persistence
  - Scalable design

## Technology Stack

- Java 11
- Spring Boot 2.7
- MySQL 8.0
- Redis
- Maven

## Getting Started

### Prerequisites

- JDK 11 or later
- MySQL 8.0
- Redis
- Maven 3.6 or later

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/jobflow.git
   ```

2. Configure the application:
   - Update `src/main/resources/application.properties` with your database and Redis settings
   - Configure email and WeChat credentials if needed

3. Initialize the database:
   ```bash
   mysql -u root -p < src/main/resources/db/init.sql
   ```

4. Build the project:
   ```bash
   mvn clean package
   ```

5. Run the application:
   ```bash
   java -jar target/job-flow-1.0.0-SNAPSHOT.jar
   ```

### Default Credentials

- Username: admin
- Password: admin123

## Configuration

The application can be configured through `application.properties`. Key configuration areas include:

- Database connection
- Redis settings
- Email configuration
- WeChat integration
- Task execution parameters
- Security settings

## API Documentation

API documentation is available through Swagger UI at:
```
http://localhost:8080/jobflow/swagger-ui.html
```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
