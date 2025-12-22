# API Gateway - Shadow Ledger System

## Overview
This API Gateway serves as the single entry point for all client requests in the Shadow Ledger System. It implements JWT-based authentication and Role-Based Access Control (RBAC) for securing access to backend microservices.

## Architecture Components

### Services
1. **API Gateway** (Port 8080) - Entry point with JWT authentication and RBAC
2. **Event Service** (Port 8081) - Handles transaction events
3. **Shadow Ledger Service** (Port 8082) - Maintains shadow balances
4. **Drift and Correction Service** (Port 8083) - Detects and corrects drift

### Infrastructure
- **Kafka** (Port 9092) - Message broker for event streaming
- **PostgreSQL** - Database instances for each service
- **Zookeeper** (Port 2181) - Kafka coordination

## Security & RBAC

### Roles and Permissions
- **user**: Can access POST `/events` only
- **auditor**: Can access POST `/drift-check` and GET `/accounts/{accountId}/shadow-balance`
- **admin**: Can access POST `/correct/{accountId}` plus auditor permissions

### Endpoints

#### Public Endpoints (No JWT Required)
- `POST /auth/token` - Generate JWT token
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Metrics

#### Protected Endpoints (JWT Required)
- `POST /events` - Submit transaction events (requires `user` role)
- `GET /accounts/{accountId}/shadow-balance` - Get shadow balance (requires `auditor` or `admin` role)
- `POST /drift-check` - Check for drift (requires `auditor` or `admin` role)
- `POST /correct/{accountId}` - Manual correction (requires `admin` role)

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17 (for local development)
- Gradle (for building)

### Running with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f api-gateway

# Stop all services
docker-compose down
```

### Building Locally

```bash
# Build the project
./gradlew build

# Run the API Gateway
./gradlew bootRun
```

## Testing the API

### Step 1: Generate JWT Tokens

#### Generate token for 'user' role:
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_user",
    "roles": ["ROLE_user"]
  }'
```

#### Generate token for 'auditor' role:
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_auditor",
    "roles": ["ROLE_auditor"]
  }'
```

#### Generate token for 'admin' role:
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin_user",
    "roles": ["ROLE_admin"]
  }'
```

### Step 2: Test Protected Endpoints

#### Test Event Submission (user role)
```bash
export USER_TOKEN="<token-from-step-1>"

curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500,
    "timestamp": 1735561800000
  }'
```

#### Test Shadow Balance Retrieval (auditor role)
```bash
export AUDITOR_TOKEN="<token-from-step-1>"

curl -X GET http://localhost:8080/accounts/A10/shadow-balance \
  -H "Authorization: Bearer $AUDITOR_TOKEN"
```

#### Test Drift Check (auditor role)
```bash
curl -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    { "accountId": "A10", "reportedBalance": 700 },
    { "accountId": "A11", "reportedBalance": 1550 }
  ]'
```

#### Test Manual Correction (admin role)
```bash
export ADMIN_TOKEN="<token-from-step-1>"

curl -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "CORR-A10-1",
    "accountId": "A10",
    "type": "credit",
    "amount": 50
  }'
```

### Step 3: Test RBAC (Expected to Fail)

#### User trying to access drift-check (403 Forbidden)
```bash
curl -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"accountId": "A10", "reportedBalance": 700}]'
```

#### Auditor trying to access correction endpoint (403 Forbidden)
```bash
curl -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
```

## Features

### 1. JWT Authentication
- All requests (except `/auth/token` and actuator endpoints) require a valid JWT token
- Token must be passed in the `Authorization` header as `Bearer <token>`
- Tokens expire after 24 hours

### 2. Role-Based Access Control (RBAC)
- Gateway enforces role-based permissions at the routing level
- Returns 401 for missing/invalid tokens
- Returns 403 for insufficient permissions

### 3. Request Tracing
- All forwarded requests include an `X-Trace-Id` header
- If not provided by the client, a UUID is automatically generated
- Enables end-to-end request tracking across microservices

### 4. Observability
- Health endpoint: `http://localhost:8080/actuator/health`
- Metrics endpoint: `http://localhost:8080/actuator/metrics`
- All logs include timestamps

### 5. Error Handling
- Clear error messages for authentication failures (401)
- Clear error messages for authorization failures (403)
- Structured JSON error responses

## Configuration

### application.yml
Key configurations:
- `server.port`: API Gateway port (default: 8080)
- `spring.cloud.gateway.routes`: Route definitions for backend services
- `jwt.secret`: Secret key for JWT signing
- `jwt.expiration`: Token expiration time in milliseconds

### Environment Variables (Docker)
- `SPRING_PROFILES_ACTIVE`: Set to `docker` for containerized deployment
- Backend service URLs are automatically resolved via Docker service names

## Monitoring & Health Checks

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

## Kafka Topics

The system uses the following Kafka topics:
- `transactions.raw` - Raw transaction events from Event Service
- `transactions.corrections` - Correction events from Drift/Correction Service

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Token is missing, invalid, or expired
   - Solution: Generate a new token using `/auth/token`

2. **403 Forbidden**: User doesn't have required role
   - Solution: Generate token with appropriate role

3. **Service not reachable**: Backend service is down
   - Solution: Check Docker Compose logs: `docker-compose logs <service-name>`

4. **Kafka connection issues**: Kafka not ready
   - Solution: Wait for Kafka to be fully started (check with `docker-compose logs kafka`)

## Development Notes

### Adding New Routes
Edit `src/main/resources/application.yml` and add route configuration under `spring.cloud.gateway.routes`.

### Modifying RBAC Rules
Edit `src/main/java/com/example/config/SecurityConfig.java` to change role-based access rules.

### Changing JWT Secret
Update `jwt.secret` in `application.yml` (ensure it's at least 256 bits for HS256).

## License
MIT License

