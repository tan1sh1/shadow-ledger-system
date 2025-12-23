# Drift Correction Service - Quick Reference

## 🚀 Quick Start

```bash
# Build and run
./gradlew bootRun

# Access service
http://localhost:8083
```

## 📍 Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/drift-check` | POST | Batch drift checking |
| `/correct/{accountId}` | POST | Manual correction |
| `/actuator/health` | GET | Health check |
| `/actuator/metrics` | GET | Metrics |

## 🔍 Drift Detection Logic

```
Drift = CBS Balance - Shadow Balance

Drift > 0  → CREDIT correction (Shadow too low)
Drift < 0  → DEBIT correction (Shadow too high)
Drift = 0  → No correction needed
```

## 📊 Example Requests

### Check Drift (Batch)
```bash
curl -X POST http://localhost:8083/drift-check \
  -H "Content-Type: application/json" \
  -d '[
    {
      "accountId": "acc-001",
      "reportedBalance": 1500.00
    },
    {
      "accountId": "acc-002",
      "reportedBalance": 2000.00
    }
  ]'
```

### Manual Correction
```bash
curl -X POST "http://localhost:8083/correct/acc-001?amount=500.00"
```

### Health Check
```bash
curl http://localhost:8083/actuator/health
```

## 🎯 Drift Scenarios

### Scenario 1: Positive Drift
```
CBS:    $1500.00
Shadow: $1000.00
Drift:  +$500.00
→ CREDIT correction for $500.00
```

### Scenario 2: Negative Drift
```
CBS:    $800.00
Shadow: $1000.00
Drift:  -$200.00
→ DEBIT correction for $200.00
```

### Scenario 3: No Drift
```
CBS:    $1000.00
Shadow: $1000.00
Drift:  $0.00
→ No correction
```

### Scenario 4: Account Not Found
```
CBS:    $1000.00
Shadow: Not Found
→ No correction
```

## 📤 Correction Event Format

### Automatic Correction
```json
{
  "eventId": "CORR-550e8400-e29b-41d4-a716-446655440000",
  "accountId": "acc-001",
  "type": "credit",
  "amount": 500.00
}
```

### Manual Correction
```json
{
  "eventId": "MANUAL-550e8400-e29b-41d4-a716-446655440000",
  "accountId": "acc-001",
  "type": "credit",
  "amount": 500.00
}
```

**Kafka Topic:** `transactions.corrections`  
**Partition Key:** `accountId`

## 🔧 Common Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test

# View test report
open build/reports/tests/test/index.html

# Docker build
docker build -t drift-correction-service .

# Docker run
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/ledgerdb \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  drift-correction-service
```

## ⚙️ Configuration

### Local (application.yml)
```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ledgerdb
    username: postgres
    password: 0000
    
  kafka:
    bootstrap-servers: localhost:9092
```

### Environment Variables
```bash
export SERVER_PORT=8083
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/ledgerdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=0000
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 🐳 Docker

```bash
# Build image
docker build -t drift-correction-service .

# Run container
docker run -d \
  --name drift-correction \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ledgerdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  drift-correction-service

# View logs
docker logs -f drift-correction

# Stop container
docker stop drift-correction
```

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8083 in use | `lsof -i :8083` or change port |
| Database connection failed | Check PostgreSQL running and credentials |
| Kafka connection failed | Check Kafka running and bootstrap servers |
| No corrections generated | Account may not exist in shadow ledger |
| Query fails | Check `ledger_entries` table exists |

### Enable Debug Logging
```yaml
logging:
  level:
    com.shadowledger.drift: DEBUG
    org.springframework.kafka: DEBUG
```

## 📊 Monitoring

```bash
# Health check
curl http://localhost:8083/actuator/health

# All metrics
curl http://localhost:8083/actuator/metrics

# Specific metric
curl http://localhost:8083/actuator/metrics/http.server.requests
```

## 🧪 Testing

```bash
# Run all tests (82+ test cases)
./gradlew test

# Run specific test
./gradlew test --tests "DriftDetectionServiceTest"

# View test report
open build/reports/tests/test/index.html
```

### Test Coverage
- ✅ 82+ comprehensive test cases
- ✅ Controllers, Services, Repository
- ✅ Integration tests
- ✅ Model tests
- ✅ Edge cases covered

## 📚 Key Components

| Component | Purpose |
|-----------|---------|
| `DriftCheckController` | Batch drift checking endpoint |
| `ManualCorrectionController` | Manual correction endpoint |
| `DriftDetectionService` | Core drift detection logic |
| `CorrectionPublisher` | Kafka message publisher |
| `ShadowLedgerRepository` | Query shadow ledger DB |

## 🎯 Integration Examples

### Python
```python
import requests

# Check drift
requests.post('http://localhost:8083/drift-check', json=[
    {'accountId': 'acc-001', 'reportedBalance': 1500.00}
])

# Manual correction
requests.post('http://localhost:8083/correct/acc-001', 
              params={'amount': 500.00})
```

### cURL with Variables
```bash
# Set base URL
BASE_URL="http://localhost:8083"

# Check drift
curl -X POST $BASE_URL/drift-check \
  -H "Content-Type: application/json" \
  -d '[{"accountId":"acc-001","reportedBalance":1500.00}]'

# Manual correction
ACCOUNT_ID="acc-001"
AMOUNT="500.00"
curl -X POST "$BASE_URL/correct/$ACCOUNT_ID?amount=$AMOUNT"
```

## 📝 Key Features

- ✅ Automatic Drift Detection
- ✅ Manual Corrections
- ✅ Kafka Integration
- ✅ Database Integration
- ✅ Health Monitoring
- ✅ 82+ Test Cases
- ✅ Docker Ready

---
See [README.md](README.md) for full documentation.  
See [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md) for test details.

