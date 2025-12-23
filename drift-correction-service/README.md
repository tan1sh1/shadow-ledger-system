# Drift Correction Service - Shadow Ledger System

## Overview

The Drift Correction Service is a critical component of the Shadow Ledger System that detects and corrects discrepancies (drift) between the Core Banking System (CBS) balances and the Shadow Ledger balances. It ensures data consistency across systems by automatically generating correction events when drift is detected.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Drift Detection Logic](#drift-detection-logic)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Development](#development)

## Features

- ✅ **Automatic Drift Detection** - Compare CBS and Shadow Ledger balances
- ✅ **Correction Event Generation** - Create credit/debit corrections automatically
- ✅ **Manual Correction Support** - API for manual balance adjustments
- ✅ **Kafka Integration** - Publish corrections to Kafka topics
- ✅ **Database Integration** - Query Shadow Ledger from PostgreSQL
- ✅ **Health Monitoring** - Actuator endpoints for health checks
- ✅ **Comprehensive Testing** - 82+ test cases covering all scenarios
- ✅ **Docker Support** - Containerized deployment ready

## Architecture

### System Flow

```
┌─────────────────────────────────────────────────────────┐
│         Drift Correction Service (Port: 8083)           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────┐      ┌──────────────────┐       │
│  │ DriftCheck       │      │ Manual           │       │
│  │ Controller       │      │ Correction       │       │
│  │ POST /drift-check│      │ POST /correct    │       │
│  └────────┬─────────┘      └────────┬─────────┘       │
│           │                         │                  │
│           ▼                         ▼                  │
│  ┌────────────────────────────────────────────┐       │
│  │    Drift Detection Service                 │       │
│  │    • Compare CBS vs Shadow balances        │       │
│  │    • Calculate drift amount                │       │
│  │    • Determine correction type             │       │
│  └────────────────┬───────────────────────────┘       │
│                   │                                    │
│                   ▼                                    │
│  ┌────────────────────────────────────────────┐       │
│  │    Correction Publisher                    │       │
│  │    • Generate correction events            │       │
│  │    • Publish to Kafka                      │       │
│  └────────────────┬───────────────────────────┘       │
│                   │                                    │
└───────────────────┼────────────────────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Kafka Topic          │
         │  transactions.        │
         │  corrections          │
         └──────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Shadow Ledger       │
         │  Service             │
         └──────────────────────┘
```

### Components

| Component | Purpose |
|-----------|---------|
| `DriftCheckController` | REST endpoint for batch drift checking |
| `ManualCorrectionController` | REST endpoint for manual corrections |
| `DriftDetectionService` | Core drift detection and correction logic |
| `CorrectionPublisher` | Kafka message publisher for corrections |
| `ShadowLedgerRepository` | Query shadow ledger balances from database |

## Prerequisites

- **Java 21** or higher
- **Gradle 8.5** or higher
- **PostgreSQL 12+** (for Shadow Ledger database)
- **Apache Kafka** (for event publishing)
- **Docker** (optional, for containerized deployment)

## Getting Started

### 1. Clone the Repository

```bash
cd shadow-ledger-system/drift-correction-service
```

### 2. Configure Database and Kafka

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ledgerdb
    username: postgres
    password: 0000
    
  kafka:
    bootstrap-servers: localhost:9092
```

### 3. Build the Project

```bash
./gradlew build
```

### 4. Run the Application

```bash
./gradlew bootRun
```

The service will start on **http://localhost:8083**

### 5. Verify Health

```bash
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Configuration

### Application Configuration (`application.yml`)

#### Server Configuration
```yaml
server:
  port: 8083
```

#### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ledgerdb
    username: postgres
    password: 0000
    driver-class-name: org.postgresql.Driver
```

#### Kafka Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | 8083 |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | jdbc:postgresql://localhost:5433/ledgerdb |
| `SPRING_DATASOURCE_USERNAME` | Database username | postgres |
| `SPRING_DATASOURCE_PASSWORD` | Database password | 0000 |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | localhost:9092 |

## API Endpoints

### 1. Drift Check (Batch)

Check for drift across multiple accounts.

```http
POST /drift-check
Content-Type: application/json

[
  {
    "accountId": "acc-001",
    "reportedBalance": 1500.00
  },
  {
    "accountId": "acc-002",
    "reportedBalance": 2000.00
  }
]
```

**Response:** `200 OK` (no body)

**Behavior:**
- Compares each CBS balance with Shadow Ledger balance
- Generates correction events for accounts with drift
- Publishes corrections to Kafka topic

### 2. Manual Correction

Manually correct an account balance.

```http
POST /correct/{accountId}?amount={amount}
```

**Example:**
```bash
curl -X POST "http://localhost:8083/correct/acc-001?amount=500.00"
```

**Response:** `200 OK` (no body)

**Behavior:**
- Creates a manual correction event (MANUAL-{UUID})
- Always creates a CREDIT correction
- Publishes to Kafka topic

### 3. Health Check

```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### 4. Metrics

```http
GET /actuator/metrics
```

**Response:** Available metrics list

## Drift Detection Logic

### How Drift is Calculated

```
Drift = CBS Balance - Shadow Balance

If Drift > 0:  Shadow is lower  → Generate CREDIT correction
If Drift < 0:  Shadow is higher → Generate DEBIT correction
If Drift = 0:  No drift         → No correction needed
```

### Example Scenarios

#### Scenario 1: Positive Drift (CBS Higher)
```
CBS Balance:    $1500.00
Shadow Balance: $1000.00
Drift:          +$500.00
→ Action: Generate CREDIT correction for $500.00
```

#### Scenario 2: Negative Drift (Shadow Higher)
```
CBS Balance:    $800.00
Shadow Balance: $1000.00
Drift:          -$200.00
→ Action: Generate DEBIT correction for $200.00
```

#### Scenario 3: No Drift
```
CBS Balance:    $1000.00
Shadow Balance: $1000.00
Drift:          $0.00
→ Action: No correction needed
```

#### Scenario 4: Account Not Found
```
CBS Balance:    $1000.00
Shadow Balance: Not Found
→ Action: No correction (account doesn't exist in shadow ledger)
```

### Correction Event Format

```json
{
  "eventId": "CORR-550e8400-e29b-41d4-a716-446655440000",
  "accountId": "acc-001",
  "type": "credit",
  "amount": 500.00
}
```

**Event ID Prefixes:**
- `CORR-` - Automatic drift correction
- `MANUAL-` - Manual correction

### Kafka Topic

Corrections are published to: **`transactions.corrections`**

Partition key: `accountId` (ensures ordering per account)

## Deployment

### Local Deployment

```bash
# Build
./gradlew build

# Run
java -jar build/libs/drift-correction-service-0.0.1.jar
```

### Docker Deployment

#### Build Docker Image
```bash
docker build -t drift-correction-service:latest .
```

#### Run Container
```bash
docker run -d \
  --name drift-correction-service \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ledgerdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  drift-correction-service:latest
```

### Docker Compose

```yaml
version: '3.8'

services:
  drift-correction-service:
    build: .
    ports:
      - "8083:8083"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ledgerdb
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=password
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - postgres
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
```

Run with:
```bash
docker-compose up -d
```

## Monitoring

### Health Checks

```bash
# Basic health
curl http://localhost:8083/actuator/health

# Detailed health (if configured)
curl http://localhost:8083/actuator/health | jq
```

### Metrics

```bash
# All metrics
curl http://localhost:8083/actuator/metrics

# Specific metric
curl http://localhost:8083/actuator/metrics/http.server.requests
```

### Logs

View application logs:
```bash
# Docker
docker logs -f drift-correction-service

# Local
tail -f logs/application.log
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Test Coverage

The service includes **82+ comprehensive test cases** covering:

| Test Category | Test Count | File |
|--------------|------------|------|
| Controller Tests | 10 | DriftCheckControllerTest, ManualCorrectionControllerTest |
| Service Tests | 22 | DriftDetectionServiceTest, CorrectionPublisherTest |
| Repository Tests | 10 | ShadowLedgerRepositoryTest |
| Integration Tests | 12 | DriftCorrectionIntegrationTest |
| Model Tests | 28 | CorrectionEventTest, CbsBalanceTest, ShadowBalanceViewTest |

### View Test Report

```bash
open build/reports/tests/test/index.html
```

### Test Scenarios Covered

✅ Positive drift (CBS > Shadow)  
✅ Negative drift (Shadow > CBS)  
✅ No drift (CBS = Shadow)  
✅ Account not found  
✅ Zero balances  
✅ Negative balances  
✅ Large amounts (999999999.99)  
✅ Small amounts (0.01)  
✅ Decimal precision  
✅ High volume (50-100 accounts)  
✅ Manual corrections  

See [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md) for detailed test documentation.

## Troubleshooting

### Common Issues

#### 1. Service Won't Start
**Problem:** Port 8083 already in use

**Solution:**
```bash
# Find process using port 8083
lsof -i :8083

# Kill process or change port in application.yml
server:
  port: 8084
```

#### 2. Database Connection Failed
**Problem:** Cannot connect to PostgreSQL

**Solution:**
- Verify PostgreSQL is running: `pg_isready -h localhost -p 5433`
- Check connection URL, username, and password
- Verify database exists: `psql -h localhost -p 5433 -U postgres -l`
- Check firewall/network settings

#### 3. Kafka Connection Failed
**Problem:** Cannot connect to Kafka

**Solution:**
- Verify Kafka is running: `kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check bootstrap server configuration
- Ensure topic exists: `transactions.corrections`
- Check Kafka logs

#### 4. No Correction Events Generated
**Problem:** Drift detected but no events published

**Possible Causes:**
- Shadow ledger account doesn't exist (expected behavior)
- Drift is exactly zero
- Kafka producer error (check logs)

**Solution:**
```bash
# Check logs for errors
tail -f logs/application.log | grep -i error

# Verify Kafka topic exists
kafka-topics.sh --describe --topic transactions.corrections --bootstrap-server localhost:9092

# Test manual correction
curl -X POST "http://localhost:8083/correct/acc-001?amount=100.00"
```

#### 5. Shadow Balance Query Fails
**Problem:** Error querying shadow ledger

**Solution:**
- Verify `ledger_entries` table exists
- Check table schema matches expected format
- Verify database user has SELECT permissions
- Test query directly in PostgreSQL

### Debug Logging

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.shadowledger.drift: DEBUG
    org.springframework.kafka: DEBUG
    org.springframework.jdbc: DEBUG
```

## Development

### Project Structure

```
drift-correction-service/
├── src/
│   ├── main/
│   │   ├── java/com/shadowledger/drift/
│   │   │   ├── DriftCorrectionApplication.java    # Main entry point
│   │   │   ├── controller/
│   │   │   │   ├── DriftCheckController.java      # Drift check endpoint
│   │   │   │   └── ManualCorrectionController.java # Manual correction
│   │   │   ├── service/
│   │   │   │   ├── DriftDetectionService.java     # Core logic
│   │   │   │   └── CorrectionPublisher.java       # Kafka publisher
│   │   │   ├── repository/
│   │   │   │   └── ShadowLedgerRepository.java    # Database queries
│   │   │   └── model/
│   │   │       ├── CbsBalance.java                # CBS balance DTO
│   │   │       ├── CorrectionEvent.java           # Correction event
│   │   │       └── ShadowBalanceView.java         # Shadow balance
│   │   └── resources/
│   │       └── application.yml                    # Configuration
│   └── test/
│       └── java/com/shadowledger/drift/           # Test files
├── build.gradle                                    # Build configuration
├── Dockerfile                                      # Docker image
├── TEST_DOCUMENTATION.md                           # Test docs
└── README.md                                       # This file
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `DriftCheckController` | REST endpoint for batch drift checking |
| `ManualCorrectionController` | REST endpoint for manual corrections |
| `DriftDetectionService` | Compares balances and generates corrections |
| `CorrectionPublisher` | Publishes correction events to Kafka |
| `ShadowLedgerRepository` | Queries shadow ledger balances |
| `CbsBalance` | DTO for CBS reported balance |
| `CorrectionEvent` | Correction event model |
| `ShadowBalanceView` | View of shadow ledger balance |

### Adding New Features

1. **Add new endpoint:**
   - Create controller method
   - Add service logic
   - Write tests

2. **Modify drift logic:**
   - Update `DriftDetectionService.checkAndCorrect()`
   - Add/update tests
   - Document behavior

3. **Add new correction type:**
   - Extend `CorrectionEvent` model
   - Update publisher logic
   - Update downstream consumers

### Code Style

- Follow Java naming conventions
- Use Lombok for getters/setters/builders
- Write unit tests for all service methods
- Include JavaDoc for public APIs
- Log important events at appropriate levels

## Performance Tuning

### Database Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### Kafka Producer Settings

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      buffer-memory: 33554432
      compression-type: gzip
```

### JVM Options

```bash
java -Xmx512m -Xms256m -XX:+UseG1GC -jar drift-correction-service.jar
```

## Security Best Practices

1. ✅ Use environment variables for sensitive data
2. ✅ Implement authentication/authorization (via API Gateway)
3. ✅ Encrypt database passwords
4. ✅ Use SSL/TLS for database connections
5. ✅ Enable Kafka security (SASL/SSL)
6. ✅ Regular security audits
7. ✅ Keep dependencies updated
8. ✅ Implement rate limiting

## API Integration Examples

### Python Example

```python
import requests

# Drift check
response = requests.post(
    'http://localhost:8083/drift-check',
    json=[
        {'accountId': 'acc-001', 'reportedBalance': 1500.00},
        {'accountId': 'acc-002', 'reportedBalance': 2000.00}
    ]
)
print(f"Status: {response.status_code}")

# Manual correction
response = requests.post(
    'http://localhost:8083/correct/acc-001',
    params={'amount': 500.00}
)
print(f"Status: {response.status_code}")
```

### Java Example

```java
RestTemplate restTemplate = new RestTemplate();

// Drift check
List<CbsBalance> balances = Arrays.asList(
    new CbsBalance("acc-001", new BigDecimal("1500.00")),
    new CbsBalance("acc-002", new BigDecimal("2000.00"))
);

ResponseEntity<Void> response = restTemplate.postForEntity(
    "http://localhost:8083/drift-check",
    balances,
    Void.class
);

// Manual correction
String url = "http://localhost:8083/correct/acc-001?amount=500.00";
restTemplate.postForEntity(url, null, Void.class);
```

### cURL Examples

See [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for more examples.

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/new-feature`
3. Write tests for new functionality
4. Commit changes: `git commit -am 'Add new feature'`
5. Push to branch: `git push origin feature/new-feature`
6. Submit pull request

## License

[Specify your license here]

## Support

For issues and questions:
- Create an issue in the repository
- Contact: [your-email@example.com]
- Documentation: See [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md)

---

**Version:** 0.0.1  
**Last Updated:** December 22, 2024  
**Maintainer:** Shadow Ledger Team

