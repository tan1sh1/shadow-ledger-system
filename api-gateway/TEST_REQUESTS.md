# API Test Requests

## 1. Generate JWT Tokens

### User Token (can access /events)
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_user",
    "roles": ["ROLE_user"]
  }'
```

### Auditor Token (can access /drift-check and /accounts)
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_auditor",
    "roles": ["ROLE_auditor"]
  }'
```

### Admin Token (can access all endpoints)
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin_user",
    "roles": ["ROLE_admin"]
  }'
```

## 2. Test Endpoints

### Submit Event (requires user role)
```bash
# Set your token first
export USER_TOKEN="YOUR_USER_TOKEN_HERE"

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

### Get Shadow Balance (requires auditor or admin role)
```bash
# Set your token first
export AUDITOR_TOKEN="YOUR_AUDITOR_TOKEN_HERE"

curl -X GET http://localhost:8080/accounts/A10/shadow-balance \
  -H "Authorization: Bearer $AUDITOR_TOKEN"
```

### Check Drift (requires auditor or admin role)
```bash
curl -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    { "accountId": "A10", "reportedBalance": 700 },
    { "accountId": "A11", "reportedBalance": 1550 }
  ]'
```

### Manual Correction (requires admin role only)
```bash
# Set your token first
export ADMIN_TOKEN="YOUR_ADMIN_TOKEN_HERE"

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

## 3. Test RBAC (Expected Failures)

### User trying to access drift-check (should return 403)
```bash
curl -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"accountId": "A10", "reportedBalance": 700}]'
```

### Auditor trying to access correction (should return 403)
```bash
curl -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "CORR-A10-1",
    "accountId": "A10",
    "type": "credit",
    "amount": 50
  }'
```

### Missing Authorization header (should return 401)
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500
  }'
```

## 4. Health and Metrics

### Health Check (public endpoint)
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (public endpoint)
```bash
curl http://localhost:8080/actuator/metrics
```

