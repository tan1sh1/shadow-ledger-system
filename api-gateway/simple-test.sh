#!/bin/bash

# Simple test commands for API Gateway with correct payload format

echo "=== Testing API Gateway ==="
echo ""

# Test 1: Health check
echo "1. Health Check:"
curl -s http://localhost:8080/actuator/health | jq '.' || echo "Service not responding"
echo ""
echo ""

# Test 2: Generate USER token
echo "2. Generate USER Token:"
USER_RESPONSE=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "roles": ["ROLE_user"]}')
echo "$USER_RESPONSE" | jq '.'
USER_TOKEN=$(echo "$USER_RESPONSE" | jq -r '.token')
echo ""

# Test 3: Generate AUDITOR token
echo "3. Generate AUDITOR Token:"
AUDITOR_RESPONSE=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_auditor", "roles": ["ROLE_auditor"]}')
echo "$AUDITOR_RESPONSE" | jq '.'
AUDITOR_TOKEN=$(echo "$AUDITOR_RESPONSE" | jq -r '.token')
echo ""

# Test 4: Generate ADMIN token
echo "4. Generate ADMIN Token:"
ADMIN_RESPONSE=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_admin", "roles": ["ROLE_admin"]}')
echo "$ADMIN_RESPONSE" | jq '.'
ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | jq -r '.token')
echo ""

# Test 5: Test without token (should get 401)
echo "5. Request without token (should fail with 401):"
curl -s -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": '$(date +%s000)'
  }' | jq '.' || echo "Failed as expected"
echo ""

# Test 6: Submit event with USER token (correct format)
echo "6. Submit Event with USER Token (correct payload):"
CURRENT_TIMESTAMP=$(date +%s000)
curl -s -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": \"E1001\",
    \"accountId\": \"A10\",
    \"type\": \"credit\",
    \"amount\": 500.00,
    \"timestamp\": $CURRENT_TIMESTAMP
  }" | jq '.'
echo ""

# Test 7: USER trying to access drift-check (should get 403)
echo "7. USER trying to access /drift-check (should fail with 403):"
curl -s -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"accountId": "A10", "reportedBalance": 700}
  ]' | jq '.'
echo ""

# Test 8: AUDITOR accessing drift-check
echo "8. AUDITOR accessing /drift-check:"
curl -s -X POST http://localhost:8080/drift-check \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"accountId": "A10", "reportedBalance": 700},
    {"accountId": "A11", "reportedBalance": 1550}
  ]' | jq '.'
echo ""

# Test 9: AUDITOR trying to access /correct (should get 403)
echo "9. AUDITOR trying to access /correct (should fail with 403):"
curl -s -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "CORR-A10-1",
    "accountId": "A10",
    "type": "credit",
    "amount": 50.00
  }' | jq '.'
echo ""

# Test 10: ADMIN accessing /correct
echo "10. ADMIN accessing /correct:"
curl -s -X POST http://localhost:8080/correct/A10 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "CORR-A10-1",
    "accountId": "A10",
    "type": "credit",
    "amount": 50.00
  }' | jq '.'
echo ""

# Test 11: ADMIN accessing shadow balance
echo "11. ADMIN accessing /accounts/A10/shadow-balance:"
curl -s -X GET http://localhost:8080/accounts/A10/shadow-balance \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'
echo ""

echo "=== Test Complete ==="
echo ""
echo "Tokens for manual testing:"
echo "USER_TOKEN=$USER_TOKEN"
echo "AUDITOR_TOKEN=$AUDITOR_TOKEN"
echo "ADMIN_TOKEN=$ADMIN_TOKEN"

