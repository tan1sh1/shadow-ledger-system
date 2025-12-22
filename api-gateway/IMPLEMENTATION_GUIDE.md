# API Gateway Service - Complete Implementation Guide

## Project Structure

```
api-gateway/
├── src/
│   └── main/
│       ├── java/com/example/
│       │   ├── Main.java                          # Main Spring Boot application
│       │   ├── config/
│       │   │   ├── SecurityConfig.java            # JWT & RBAC configuration
│       │   │   ├── GlobalErrorHandler.java        # Custom error handling
│       │   │   └── JacksonConfig.java             # JSON serialization config
│       │   ├── controller/
│       │   │   └── AuthController.java            # Token generation endpoint
│       │   ├── filter/
│       │   │   └── TraceIdFilter.java             # X-Trace-Id header injection
│       │   ├── security/
│       │   │   └── JwtAuthenticationFilter.java   # JWT validation filter
│       │   └── util/
│       │       └── JwtUtil.java                   # JWT token utilities
│       └── resources/
│           └── application.yml                     # Application configuration
├── build.gradle                                    # Gradle build configuration
├── Dockerfile                                      # Docker image definition
├── docker-compose.yml                              # Multi-service orchestration
├── README.md                                       # User documentation
├── TEST_REQUESTS.md                                # API test examples
└── build-and-test.sh                              # Build automation script
```

## Implementation Details

### 1. Security Configuration (SecurityConfig.java)

**Purpose**: Configures Spring Security with WebFlux for reactive gateway security.

**Key Features**:
- Disables CSRF, HTTP Basic, and Form Login (since we use JWT)
- Uses stateless security context (no sessions)
- Defines path-based authorization rules:
  - `/auth/token` - Public (no authentication)
  - `/actuator/**` - Public (for health checks)
  - `/events` - Requires `ROLE_user`
  - `/accounts/**` - Requires `ROLE_auditor` or `ROLE_admin`
  - `/drift-check` - Requires `ROLE_auditor` or `ROLE_admin`
  - `/correct/**` - Requires `ROLE_admin`

**RBAC Matrix**:
```
Endpoint                 | user | auditor | admin
-------------------------|------|---------|-------
POST /events            |  ✓   |    ✗    |   ✗
GET /accounts/{id}/...  |  ✗   |    ✓    |   ✓
POST /drift-check       |  ✗   |    ✓    |   ✓
POST /correct/{id}      |  ✗   |    ✗    |   ✓
```

### 2. JWT Authentication Filter (JwtAuthenticationFilter.java)

**Purpose**: Intercepts all requests to validate JWT tokens before routing.

**Flow**:
1. Extract `Authorization` header from request
2. Check if endpoint is public (skip validation)
3. Validate Bearer token format
4. Parse and validate JWT signature and expiration
5. Extract username and roles from token
6. Create Spring Security authentication context
7. Allow request to proceed or return 401

**Error Handling**:
- Missing token → 401 Unauthorized
- Invalid token → 401 Unauthorized
- Expired token → 401 Unauthorized

### 3. Trace ID Filter (TraceIdFilter.java)

**Purpose**: Adds distributed tracing capability by injecting `X-Trace-Id` header.

**Behavior**:
- If request has `X-Trace-Id`, forwards it to backend services
- If missing, generates a new UUID and adds it
- Logs the trace ID for correlation
- Enables end-to-end request tracking across microservices

### 4. JWT Utility (JwtUtil.java)

**Purpose**: Handles all JWT operations (generation, parsing, validation).

**Methods**:
- `generateToken(username, roles)` - Creates a new JWT with claims
- `validateToken(token)` - Checks signature and expiration
- `extractUsername(token)` - Gets subject from token
- `extractRoles(token)` - Gets roles claim
- `extractAllClaims(token)` - Parses entire token payload

**Token Structure**:
```json
{
  "sub": "john_user",
  "roles": ["ROLE_user"],
  "iat": 1735561800,
  "exp": 1735648200
}
```

### 5. Auth Controller (AuthController.java)

**Purpose**: Provides `/auth/token` endpoint for generating test JWT tokens.

**Usage**: Send POST request with username and roles to get a JWT.

**Request**:
```json
{
  "username": "john_user",
  "roles": ["ROLE_user"]
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "john_user",
  "roles": "ROLE_user"
}
```

### 6. Global Error Handler (GlobalErrorHandler.java)

**Purpose**: Provides consistent error responses for security exceptions.

**Error Responses**:

**401 Unauthorized**:
```json
{
  "timestamp": "2025-12-20T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Invalid or missing JWT token",
  "path": "/events"
}
```

**403 Forbidden**:
```json
{
  "timestamp": "2025-12-20T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: You don't have the required role to access this resource",
  "path": "/drift-check"
}
```

### 7. Application Configuration (application.yml)

**Key Sections**:

**Gateway Routes**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: event-service
          uri: http://event-service:8081
          predicates:
            - Path=/events,/events/**
          filters:
            - name: TraceIdFilter
```

**JWT Configuration**:
```yaml
jwt:
  secret: "mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890"
  expiration: 86400000  # 24 hours
```

**Profiles**:
- `default` - Uses Docker service names
- `docker` - Uses Docker service names (for Docker Compose)
- `local` - Uses localhost URLs (for local development)

### 8. Docker Configuration

**Dockerfile**:
- Multi-stage build (builder + runtime)
- Uses Gradle to build JAR
- Uses Alpine-based JRE for small image size
- Includes health check endpoint
- Exposes port 8080

**docker-compose.yml**:
- Orchestrates all microservices
- Includes Kafka, Zookeeper, and PostgreSQL databases
- Configures health checks for all services
- Sets up custom network for inter-service communication
- Defines volume mounts for data persistence

## How It Works

### Request Flow

```
1. Client → API Gateway (Port 8080)
   ↓
2. JwtAuthenticationFilter validates JWT
   ↓
3. SecurityConfig checks RBAC permissions
   ↓
4. TraceIdFilter adds X-Trace-Id header
   ↓
5. Gateway routes to backend service
   ↓
6. Backend service processes request
   ↓
7. Response flows back to client
```

### Authentication Flow

```
1. Client requests token from /auth/token
   ↓
2. AuthController generates JWT with roles
   ↓
3. Client includes JWT in Authorization header
   ↓
4. JwtAuthenticationFilter validates token
   ↓
5. Spring Security context populated with roles
   ↓
6. Request authorized based on role
```

## Building and Running

### Local Development

```bash
# Build the project
./gradlew clean build -x test

# Run the application
./gradlew bootRun

# Or use the build script
chmod +x build-and-test.sh
./build-and-test.sh
```

### Docker

```bash
# Build Docker image
docker build -t api-gateway .

# Run container
docker run -p 8080:8080 api-gateway
```

### Docker Compose (All Services)

```bash
# Start all services
docker-compose up --build

# Start in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f api-gateway

# Stop all services
docker-compose down
```

## Testing

### 1. Generate Tokens

```bash
# User token
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "roles": ["ROLE_user"]}'

# Save token
export TOKEN="<paste-token-here>"
```

### 2. Test Protected Endpoints

```bash
# Submit event (requires user role)
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500,
    "timestamp": 1735561800000
  }'
```

### 3. Verify RBAC

```bash
# User trying to access admin endpoint (should fail with 403)
curl -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventId": "C001", "accountId": "A10", "type": "credit", "amount": 50}'
```

## Integration with Backend Services

### Event Service Integration
- Listens at: `http://event-service:8081`
- Endpoint: `POST /events`
- Receives: Transaction events from gateway
- Returns: Event confirmation

### Shadow Ledger Service Integration
- Listens at: `http://shadow-ledger-service:8082`
- Endpoint: `GET /accounts/{accountId}/shadow-balance`
- Receives: Balance queries from gateway
- Returns: Current shadow balance

### Drift/Correction Service Integration
- Listens at: `http://drift-correction-service:8083`
- Endpoints:
  - `POST /drift-check` - CBS balance comparison
  - `POST /correct/{accountId}` - Manual corrections
- Receives: Drift detection and correction requests
- Returns: Drift analysis and correction results

## Environment Variables

For Docker deployment, configure:

```bash
SPRING_PROFILES_ACTIVE=docker
JWT_SECRET=<your-secret-key>
JWT_EXPIRATION=86400000
```

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP"
}
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

## Security Considerations

1. **JWT Secret**: Change the default secret in production
2. **Token Expiration**: Adjust based on security requirements
3. **HTTPS**: Use HTTPS in production (configure SSL/TLS)
4. **CORS**: Restrict allowed origins in production
5. **Rate Limiting**: Consider adding rate limiting for /auth/token

## Troubleshooting

### Issue: 401 Unauthorized
- **Cause**: Missing or invalid JWT token
- **Solution**: Generate new token via `/auth/token`

### Issue: 403 Forbidden
- **Cause**: User doesn't have required role
- **Solution**: Generate token with correct role

### Issue: Service not reachable
- **Cause**: Backend service is down
- **Solution**: Check `docker-compose logs <service-name>`

### Issue: Kafka connection errors
- **Cause**: Kafka not ready
- **Solution**: Wait for Kafka to start (check health)

## Next Steps

1. **Add Rate Limiting**: Implement rate limiting per user/IP
2. **Add API Documentation**: Integrate Swagger/OpenAPI
3. **Add Caching**: Cache frequently accessed data
4. **Add Circuit Breaker**: Implement resilience patterns
5. **Add Monitoring**: Integrate Prometheus/Grafana

## Support

For issues or questions, refer to:
- README.md - User guide
- TEST_REQUESTS.md - API examples
- This document - Implementation details

