# API Gateway - Shadow Ledger System

## Overview

The API Gateway is the central entry point for the Shadow Ledger System, providing unified access to all microservices with built-in authentication, authorization, request routing, and distributed tracing capabilities.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Request Tracing](#request-tracing)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Development](#development)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Features

- ✅ **Unified API Entry Point** - Single point of access for all microservices
- ✅ **JWT-based Authentication** - Secure token-based authentication
- ✅ **Request Routing** - Intelligent routing to backend services
- ✅ **Distributed Tracing** - X-Trace-Id header for request tracking
- ✅ **CORS Support** - Cross-Origin Resource Sharing configuration
- ✅ **Health Monitoring** - Actuator endpoints for health checks
- ✅ **Global Error Handling** - Centralized error management
- ✅ **Docker Support** - Containerized deployment ready

## Architecture

### Components

```
┌─────────────────────────────────────────────────────┐
│                   API Gateway                        │
│                  (Port: 8080)                        │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │ Auth         │  │ TraceId      │  │ Security │  │
│  │ Controller   │  │ Filter       │  │ Filter   │  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────┘
         │              │              │
         ├──────────────┼──────────────┤
         │              │              │
    ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
    │ Event   │   │ Shadow  │   │ Drift   │
    │ Service │   │ Ledger  │   │ Correct │
    │  :8081  │   │  :8082  │   │  :8083  │
    └─────────┘   └─────────┘   └─────────┘
```

### Service Routes

| Route Pattern | Target Service | Port |
|--------------|----------------|------|
| `/events/**` | Event Service | 8081 |
| `/accounts/**` | Shadow Ledger Service | 8082 |
| `/drift-check/**` | Drift Correction Service | 8083 |
| `/correct/**` | Drift Correction Service | 8083 |
| `/auth/**` | Auth Controller (Gateway) | 8080 |

## Prerequisites

- **Java 17** or higher
- **Gradle 8.5** or higher
- **Docker** (optional, for containerized deployment)
- Backend services running:
  - Event Service (port 8081)
  - Shadow Ledger Service (port 8082)
  - Drift Correction Service (port 8083)

## Getting Started

### 1. Clone the Repository

```bash
cd shadow-ledger-system/api-gateway
```

### 2. Build the Project

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The API Gateway will start on **http://localhost:8080**

### 4. Verify Health

```bash
curl http://localhost:8080/actuator/health
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
  port: 8080
```

#### JWT Configuration
```yaml
jwt:
  secret: "your-secret-key-here"
  expiration: 86400000  # 24 hours
```

#### Service URLs (Local Development)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: event-service
          uri: http://localhost:8081
          predicates:
            - Path=/events,/events/**
```

#### Service URLs (Docker Deployment)
```yaml
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: event-service
          uri: http://event-service:8081
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Gateway port | 8080 |
| `JWT_SECRET` | JWT signing secret | (configured) |
| `JWT_EXPIRATION` | Token expiration (ms) | 86400000 |
| `EVENT_SERVICE_URL` | Event Service URL | http://localhost:8081 |
| `SHADOW_LEDGER_URL` | Shadow Ledger URL | http://localhost:8082 |
| `DRIFT_SERVICE_URL` | Drift Service URL | http://localhost:8083 |

## API Endpoints

### Authentication Endpoints

#### Generate JWT Token
```http
POST /auth/token
Content-Type: application/json

{
  "username": "admin",
  "roles": ["USER", "ADMIN"]
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": "USER, ADMIN"
}
```

### Proxied Service Endpoints

#### Event Service
```http
# Create event
POST /events
Authorization: Bearer <token>
Content-Type: application/json

{
  "eventId": "evt-001",
  "accountId": "acc-001",
  "type": "credit",
  "amount": 100.00,
  "timestamp": 1234567890
}
```

#### Shadow Ledger Service
```http
# Get account balance
GET /accounts/{accountId}/balance
Authorization: Bearer <token>
```

#### Drift Correction Service
```http
# Check drift
POST /drift-check
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "accountId": "acc-001",
    "reportedBalance": 1000.00
  }
]
```

```http
# Manual correction
POST /correct/{accountId}?amount=500.00
Authorization: Bearer <token>
```

### Actuator Endpoints

```http
# Health check
GET /actuator/health

# Metrics
GET /actuator/metrics

# Prometheus metrics
GET /actuator/prometheus

# Application info
GET /actuator/info
```

## Authentication

### JWT Token Flow

1. **Request Token**: Client sends username and roles to `/auth/token`
2. **Generate Token**: Gateway generates JWT with 24-hour expiration
3. **Use Token**: Client includes token in Authorization header
4. **Validate Token**: Gateway validates token on each request
5. **Forward Request**: Valid requests are forwarded to backend services

### Using JWT in Requests

```bash
# 1. Get token
TOKEN=$(curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","roles":["USER","ADMIN"]}' \
  | jq -r '.token')

# 2. Use token
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Security Features

- ✅ JWT signature validation
- ✅ Token expiration checking
- ✅ Role-based access control (configurable)
- ✅ Secure secret key management
- ✅ HTTPS support (in production)

## Request Tracing

### X-Trace-Id Header

Every request through the gateway receives a unique trace ID for distributed tracing.

**Auto-generated:**
```http
GET /accounts/acc-001/balance
Authorization: Bearer <token>

# Gateway adds: X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
```

**Custom trace ID:**
```http
GET /accounts/acc-001/balance
Authorization: Bearer <token>
X-Trace-Id: my-custom-trace-123

# Gateway forwards: X-Trace-Id: my-custom-trace-123
```

### Log Format

```
2024-12-22 10:30:45 - Request to /events with Trace-ID: 550e8400-e29b-41d4-a716-446655440000
```

## Deployment

### Local Deployment

```bash
# Build
./gradlew build

# Run
java -jar build/libs/api-gateway-1.0-SNAPSHOT.jar
```

### Docker Deployment

#### Build Docker Image
```bash
docker build -t shadow-ledger/api-gateway:latest .
```

#### Run Container
```bash
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  shadow-ledger/api-gateway:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  api-gateway:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - event-service
      - shadow-ledger-service
      - drift-correction-service
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
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
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health | jq
```

### Metrics

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Prometheus Integration

```bash
# Prometheus format metrics
curl http://localhost:8080/actuator/prometheus
```

Add to Prometheus configuration:
```yaml
scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

## Development

### Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── Main.java                    # Application entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java      # Security configuration
│   │   │   │   ├── JacksonConfig.java       # JSON configuration
│   │   │   │   └── GlobalErrorHandler.java  # Error handling
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java      # Auth endpoints
│   │   │   ├── filter/
│   │   │   │   └── TraceIdFilter.java       # Trace ID injection
│   │   │   ├── security/
│   │   │   │   └── JwtAuthenticationFilter.java # JWT validation
│   │   │   └── util/
│   │   │       └── JwtUtil.java             # JWT utilities
│   │   └── resources/
│   │       └── application.yml              # Configuration
│   └── test/
│       └── java/                            # Test files
├── build.gradle                             # Build configuration
├── Dockerfile                               # Docker image
├── docker-compose.yml                       # Docker Compose
└── README.md                                # This file
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `Main.java` | Spring Boot application entry point |
| `SecurityConfig.java` | Spring Security configuration |
| `AuthController.java` | JWT token generation endpoint |
| `JwtUtil.java` | JWT token creation and validation |
| `JwtAuthenticationFilter.java` | Request authentication filter |
| `TraceIdFilter.java` | Trace ID injection for requests |
| `GlobalErrorHandler.java` | Centralized error handling |

### Adding New Routes

Edit `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: http://localhost:8084
          predicates:
            - Path=/new-service/**
          filters:
            - name: TraceIdFilter
```

### Custom Filters

Create a new filter class:

```java
@Component
public class CustomFilter extends AbstractGatewayFilterFactory<CustomFilter.Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Pre-processing
            ServerHttpRequest request = exchange.getRequest();
            
            // Process request
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // Post-processing
                ServerHttpResponse response = exchange.getResponse();
            }));
        };
    }
    
    public static class Config {
        // Configuration properties
    }
}
```

## Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing

#### 1. Test Authentication
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","roles":["USER"]}'
```

#### 2. Test Event Creation
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-test-001",
    "accountId": "acc-001",
    "type": "credit",
    "amount": 100.00,
    "timestamp": 1703260800000
  }'
```

#### 3. Test Balance Query
```bash
curl -X GET http://localhost:8080/accounts/acc-001/balance \
  -H "Authorization: Bearer <token>"
```

### Load Testing

Using Apache Bench:
```bash
ab -n 1000 -c 10 -H "Authorization: Bearer <token>" \
  http://localhost:8080/accounts/acc-001/balance
```

## Troubleshooting

### Common Issues

#### 1. Gateway Won't Start
**Problem:** Port 8080 already in use

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill process or change port in application.yml
server:
  port: 8081
```

#### 2. Backend Service Unreachable
**Problem:** 503 Service Unavailable

**Solution:**
- Verify backend services are running
- Check service URLs in configuration
- Test direct connection: `curl http://localhost:8081/actuator/health`

#### 3. JWT Token Invalid
**Problem:** 401 Unauthorized

**Solution:**
- Verify JWT secret matches across services
- Check token expiration
- Ensure Bearer prefix in Authorization header

#### 4. CORS Errors
**Problem:** CORS policy blocking requests

**Solution:**
Update `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "https://your-domain.com"
            allowedMethods: "*"
```

### Logs

View application logs:
```bash
# Docker
docker logs api-gateway

# Local
tail -f logs/application.log
```

Enable debug logging:
```yaml
logging:
  level:
    com.example: DEBUG
    org.springframework.cloud.gateway: TRACE
```

## Performance Tuning

### JVM Options

```bash
java -Xmx512m -Xms256m -XX:+UseG1GC -jar api-gateway.jar
```

### Connection Pool

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          acquire-timeout: 45000
```

### Timeouts

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 1000
        response-timeout: 5s
```

## Security Best Practices

1. ✅ Use strong JWT secret (minimum 256 bits)
2. ✅ Enable HTTPS in production
3. ✅ Implement rate limiting
4. ✅ Use environment variables for secrets
5. ✅ Enable CORS only for trusted domains
6. ✅ Implement request/response size limits
7. ✅ Regular security audits
8. ✅ Keep dependencies updated

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/new-feature`
3. Commit changes: `git commit -am 'Add new feature'`
4. Push to branch: `git push origin feature/new-feature`
5. Submit pull request

## License

[Specify your license here]

## Support

For issues and questions:
- Create an issue in the repository
- Contact: [your-email@example.com]
- Documentation: [link-to-docs]

---

**Version:** 1.0.0  
**Last Updated:** December 22, 2024  
**Maintainer:** Shadow Ledger Team

