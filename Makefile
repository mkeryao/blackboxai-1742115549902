# JobFlow Makefile

.PHONY: help build run test clean docker-build docker-run docker-clean

# Default target
help:
	@echo "Available commands:"
	@echo "  make build        - Build the application"
	@echo "  make run         - Run the application locally"
	@echo "  make test        - Run tests"
	@echo "  make clean       - Clean build artifacts"
	@echo "  make docker-build - Build Docker images"
	@echo "  make docker-run  - Run with Docker Compose"
	@echo "  make docker-clean - Clean Docker resources"
	@echo "  make init-db     - Initialize database"
	@echo "  make update-deps - Update dependencies"
	@echo "  make lint        - Run code linting"
	@echo "  make format      - Format code"

# Build the application
build:
	@echo "Building application..."
	mvn clean package -DskipTests

# Run the application locally
run:
	@echo "Running application..."
	mvn spring-boot:run

# Run tests
test:
	@echo "Running tests..."
	mvn test

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	rm -rf logs/
	rm -rf target/

# Build Docker images
docker-build:
	@echo "Building Docker images..."
	docker-compose build

# Run with Docker Compose
docker-run:
	@echo "Starting services with Docker Compose..."
	docker-compose up -d

# Clean Docker resources
docker-clean:
	@echo "Cleaning Docker resources..."
	docker-compose down -v
	docker system prune -f

# Initialize database
init-db:
	@echo "Initializing database..."
	mysql -u root -p < src/main/resources/db/init.sql

# Update dependencies
update-deps:
	@echo "Updating dependencies..."
	mvn versions:display-dependency-updates
	mvn versions:use-latest-versions

# Run code linting
lint:
	@echo "Running code linting..."
	mvn checkstyle:check
	mvn spotbugs:check

# Format code
format:
	@echo "Formatting code..."
	mvn git-code-format:format-code

# Development environment setup
setup-dev:
	@echo "Setting up development environment..."
	mvn clean install
	cp src/main/resources/application.properties src/main/resources/application-dev.properties
	chmod +x wait-for.sh

# Generate API documentation
gen-docs:
	@echo "Generating API documentation..."
	mvn javadoc:javadoc
	mvn springdoc:generate

# Run security audit
security-audit:
	@echo "Running security audit..."
	mvn dependency-check:check
	mvn owasp:check

# Create release
release:
	@echo "Creating release..."
	mvn release:prepare
	mvn release:perform

# Monitor application
monitor:
	@echo "Opening monitoring dashboards..."
	xdg-open http://localhost:9090 # Prometheus
	xdg-open http://localhost:3000 # Grafana

# Backup database
backup-db:
	@echo "Backing up database..."
	docker exec jobflow-mysql mysqldump -u root -p job_flow > backup/job_flow_$(shell date +%Y%m%d_%H%M%S).sql

# Restore database
restore-db:
	@echo "Restoring database..."
	docker exec -i jobflow-mysql mysql -u root -p job_flow < $(file)

# Check application status
status:
	@echo "Checking application status..."
	docker-compose ps
	curl -f http://localhost:8080/actuator/health

# Stop all services
stop:
	@echo "Stopping all services..."
	docker-compose stop

# Restart all services
restart:
	@echo "Restarting all services..."
	docker-compose restart

# Show logs
logs:
	@echo "Showing logs..."
	docker-compose logs -f

# Run integration tests
integration-test:
	@echo "Running integration tests..."
	mvn verify -P integration-test

# Run performance tests
performance-test:
	@echo "Running performance tests..."
	mvn gatling:test

# Generate test coverage report
coverage:
	@echo "Generating test coverage report..."
	mvn jacoco:report
	xdg-open target/site/jacoco/index.html
