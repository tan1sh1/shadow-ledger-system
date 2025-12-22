#!/bin/bash

# Quick Manual Test Commands
# Copy and paste these commands one by one to test the API Gateway

echo "=== STEP 1: Check if service is running ==="
curl http://localhost:8080/actuator/health
echo -e "\n"

echo "=== STEP 2: Generate USER token ==="
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "roles": ["ROLE_user"]}'
echo -e "\n"

echo "=== STEP 3: Save the token and test ==="
echo "Copy the token from above and run:"
echo 'export TOKEN="<paste-token-here>"'
echo ""
echo "Then test with:"
echo 'curl -X POST http://localhost:8080/events \'
echo '  -H "Authorization: Bearer $TOKEN" \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"eventId":"E001","accountId":"A10","type":"credit","amount":100}'"'"
echo -e "\n"

echo "=== STEP 4: Test without token (should fail) ==="
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"E001","accountId":"A10","type":"credit","amount":100}'
echo -e "\n"

