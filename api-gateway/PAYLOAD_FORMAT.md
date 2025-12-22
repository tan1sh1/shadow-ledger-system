# Correct Event Payload Format for Testing

## Event Service Endpoint: POST /events

### Required Fields:
- `eventId`: String (unique identifier)
- `accountId`: String (account identifier)
- `type`: String (must be "credit" or "debit")
- `amount`: Number (must be > 0)
- `timestamp`: Long (epoch milliseconds)

## Example 1: Submit Event with Correct Format

```bash
# 1. Generate token
TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "roles": ["ROLE_user"]}')

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')

# 2. Submit event with correct payload
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1734696800000
  }'
```

## Example 2: Submit Event with Current Timestamp

```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": \"E1002\",
    \"accountId\": \"A10\",
    \"type\": \"debit\",
    \"amount\": 200.00,
    \"timestamp\": $(date +%s)000
  }"
```

## Example 3: Multiple Events

```bash
# Event 1 - Credit
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1734696800000
  }'

# Event 2 - Debit
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1002",
    "accountId": "A10",
    "type": "debit",
    "amount": 200.00,
    "timestamp": 1734696900000
  }'

# Event 3 - Credit to different account
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1003",
    "accountId": "A11",
    "type": "credit",
    "amount": 1000.00,
    "timestamp": 1734697000000
  }'
```

## Common Validation Errors

### Error: "Invalid event payload"
**Cause**: 
- Missing required field
- Wrong data type (e.g., string instead of number for amount)
- Invalid type (must be "credit" or "debit")
- Amount <= 0
- Duplicate eventId

**Solution**: Ensure all fields match the format above

### Error: "Duplicate eventId"
**Cause**: EventId already exists
**Solution**: Use a unique eventId for each event

### Error: "Amount must be positive"
**Cause**: Amount is 0 or negative
**Solution**: Use amount > 0

### Error: "Invalid type"
**Cause**: Type is not "credit" or "debit"
**Solution**: Use exactly "credit" or "debit" (case-sensitive)

## Testing Different Scenarios

### Test 1: Valid Credit Event
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1734696800000
  }'
```
Expected: 200 OK or 201 Created

### Test 2: Valid Debit Event
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1002",
    "accountId": "A10",
    "type": "debit",
    "amount": 200.00,
    "timestamp": 1734696900000
  }'
```
Expected: 200 OK or 201 Created

### Test 3: Invalid - Missing eventId (should fail)
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1734696800000
  }'
```
Expected: 400 Bad Request

### Test 4: Invalid - Negative amount (should fail)
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1003",
    "accountId": "A10",
    "type": "credit",
    "amount": -100.00,
    "timestamp": 1734696800000
  }'
```
Expected: 400 Bad Request

### Test 5: Invalid - Wrong type (should fail)
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1004",
    "accountId": "A10",
    "type": "transfer",
    "amount": 100.00,
    "timestamp": 1734696800000
  }'
```
Expected: 400 Bad Request

## Quick Copy-Paste Commands

```bash
# Set token
export TOKEN="<paste-your-token-here>"

# Submit valid event
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1734696800000
  }'
```

## Generate Timestamp (bash)
```bash
# Current timestamp in milliseconds
date +%s000

# Use in curl
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": \"E1001\",
    \"accountId\": \"A10\",
    \"type\": \"credit\",
    \"amount\": 500.00,
    \"timestamp\": $(date +%s)000
  }"
```

