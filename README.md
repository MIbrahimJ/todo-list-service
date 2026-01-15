# Todo List Service 📝

A resilient backend service for managing todo lists, built with Spring Boot and following best practices.

## Service Description

This service provides a RESTful API for managing todo items with automatic status updates. Items automatically change to "past due" when their due date passes, and past due items become immutable.

### Key Features:
-  Create, read, and update todo items
-  Automatic status updates to "past due"
-  Immutable past due items
-  Comprehensive validation
-  Docker support
-  API documentation (OpenAPI/Swagger)


## Tech Stack

- **Runtime**: Java 17
- **Framework**: Spring Boot 3.2.2
- **Database**: H2 (in-memory)
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, Spring Test
- **Documentation**: OpenAPI 3.0/Swagger
- **Validation**: Jakarta Bean Validation

## How-To Guide

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (optional)

### Build with Maven
```bash
mvn clean package
```
### Or build without tests
```bash
mvn clean package -DskipTests
```
### Start the app
```bash
 mvn spring-boot:run
```

### Build Docker image
```bash
docker build -t todo-service .
```

### Run with Docker
```bash
docker run -p 8080:8080 todo-service
```

### Or use Docker Compose
```bash
docker-compose up
```

Once running, the service will be available at:    http://localhost:8080/api

---
## API Documentation (Swagger UI)
Swagger UI: http://localhost:8080/api/swagger-ui.html

OpenAPI JSON: http://localhost:8080/api/v3/api-docs

## H2 Database Console
### URL: http://localhost:8080/api/h2-console

### JDBC URL: jdbc:h2:mem:tododb

### Username: sa

### Password: (leave empty)

---

## API Example Requests
### Create a Todo Item
```bash
curl -X POST http://localhost:8080/api/v1/todos \
-H "Content-Type: application/json" \
-d '{
"description": "Complete project documentation",
"dueDateTime": "2029-12-31T23:59:59"
}'
```

### Get All Not Done Items
```bash
curl http://localhost:8080/api/v1/todos
```

### Mark Item as Done
```bash
curl -X PATCH http://localhost:8080/api/v1/todos/1/done
```

## Monitoring
The service includes Spring Boot Actuator endpoints:

Health: http://localhost:8080/api/actuator/health

## Error Handling
The service provides consistent error responses:

400: Bad Request (validation errors)

404: Not Found (resource not found)

500: Internal Server Error