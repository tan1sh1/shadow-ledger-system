# EventService

EventService is a microservice responsible for handling event-related operations in the Shadow Ledger System. It provides REST APIs for event creation, validation, drift detection, and balance computation.

## Features
- Create and manage events
- Validate event data
- Detect drift in account balances
- Compute balances based on events
- Integration with other services via REST and messaging

## Getting Started

### Prerequisites
- Java 17+
- Gradle
- Docker (optional, for containerization)

### Build and Run

#### Using Gradle
```
./gradlew build
./gradlew bootRun
```

#### Using Docker
```
docker build -t eventservice .
docker run -p 8080:8080 eventservice
```

#### Using Docker Compose
```
docker-compose up --build
```

## API Endpoints
- `POST /events` - Create a new event
- `GET /events/{id}` - Get event by ID
- `GET /events` - List all events
- `POST /events/validate` - Validate event data
- `POST /events/drift` - Detect drift
- `GET /balance/{accountId}` - Get account balance

## Testing
Run unit and integration tests:
```
./gradlew test
```
Test reports are available in `build/reports/tests/test/index.html`.

## Configuration
Application configuration is in `src/main/resources/application.yml`.

## Project Structure
- `src/main/java/com/example/` - Main source code
- `src/test/java/com/example/` - Test code
- `build.gradle` - Build configuration
- `Dockerfile` - Container build file
- `docker-compose.yml` - Multi-service orchestration

## License
This project is licensed under the MIT License.

