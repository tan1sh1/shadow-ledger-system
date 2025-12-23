# API Gateway - Quick Reference

## 🚀 Quick Start

```bash
# Build and run
./gradlew bootRun

# Access gateway
http://localhost:8080
```

## 🔑 Authentication

```bash
# Get JWT token
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","roles":["USER","ADMIN"]}'

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": "USER, ADMIN"
}
```

## 📍 Routes

| Path | Service | Port |
|------|---------|------|
| `/events/**` | Event Service | 8081 |
| `/accounts/**` | Shadow Ledger | 8082 |
| `/drift-check/**` | Drift Correction | 8083 |
| `/correct/**` | Drift Correction | 8083 |
| `/auth/**` | Gateway Auth | 8080 |

## 🔧 Common Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test

# Docker build
docker build -t api-gateway .

# Docker run
docker run -p 8080:8080 api-gateway

# Health check
curl http://localhost:8080/actuator/health
```

## 📊 Monitoring

```bash
# Health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus
curl http://localhost:8080/actuator/prometheus
```

## 🔍 Example Requests

### Create Event
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acc-001",
    "type": "credit",
    "amount": 100.00,
    "timestamp": 1703260800000
  }'
```

### Get Account Balance
```bash
curl -X GET http://localhost:8080/accounts/acc-001/balance \
  -H "Authorization: Bearer <token>"
```

### Check Drift
```bash
curl -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[{"accountId":"acc-001","reportedBalance":1000.00}]'
```

### Manual Correction
```bash
curl -X POST "http://localhost:8080/correct/acc-001?amount=500.00" \
  -H "Authorization: Bearer <token>"
```

## ⚙️ Configuration

### Local (application.yml)
```yaml
server:
  port: 8080
jwt:
  secret: "your-secret-key"
  expiration: 86400000
```

### Environment Variables
```bash
export SERVER_PORT=8080
export JWT_SECRET="your-secret-key"
export JWT_EXPIRATION=86400000
```

## 🐳 Docker

```bash
# Build
docker build -t api-gateway .

# Run
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  api-gateway

# Logs
docker logs -f api-gateway

# Stop
docker stop api-gateway
```

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Port in use | `lsof -i :8080` or change port |
| 503 Error | Check backend services running |
| 401 Error | Verify JWT token and secret |
| CORS Error | Update CORS configuration |

## 📝 Key Features

- ✅ JWT Authentication
- ✅ Request Routing
- ✅ Trace ID Injection
- ✅ Health Monitoring
- ✅ CORS Support
- ✅ Docker Ready

---
See [README.md](README.md) for full documentation.

