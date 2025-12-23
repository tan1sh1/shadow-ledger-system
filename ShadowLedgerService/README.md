# ShadowLedgerService

ShadowLedgerService is a core microservice in the Shadow Ledger System, responsible for managing ledger operations, drift detection, correction events, and integration with other services.

## Features
- Manage shadow ledger accounts and transactions
- Detect and correct drift in account balances
- Generate correction events
- RESTful API for ledger operations
- Kafka integration for event streaming
- Validation and windowed computations

## Getting Started

### Prerequisites
- Java 17+
- Gradle
- Docker (optional)

### Build and Run

#### Using Gradle
```
./gradlew build
./gradlew bootRun
```

#### Using Docker
```
docker build -t shadowledgerservice .
docker run -p 8080:8080 shadowledgerservice
```

#### Using Docker Compose
```
docker-compose up --build
```

## API Endpoints
- `POST /ledger/transaction` - Add a transaction
- `GET /ledger/balance/{accountId}` - Get account balance
- `POST /ledger/drift` - Detect drift
- `POST /ledger/correction` - Generate correction event
- `GET /ledger/transactions` - List transactions

## Kafka Topics
- `transaction-events` - Publishes transaction events
- `correction-events` - Publishes correction events

## Testing
Run all tests:
```
./gradlew test
```
Test reports are available in `build/reports/tests/test/index.html`.

## Configuration
- Main config: `src/main/resources/application.yml`
- Kafka config: `src/main/resources/application.yml`

## Project Structure
- `src/main/java/com/banking/shadowledger/` - Main source code
- `src/test/java/com/banking/shadowledger/` - Test code
- `build.gradle` - Build configuration
- `Dockerfile` - Container build file
- `docker-compose.yml` - Multi-service orchestration

## License
This project is licensed under the MIT License.

