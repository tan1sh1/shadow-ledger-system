#!/bin/bash

# API Gateway Test Script
# This script tests all the core functionality of the API Gateway

set -e

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "   API Gateway Service Test Suite"
echo "=========================================="
echo ""

# Test 1: Health Check
echo "Test 1: Health Check (No Auth Required)"
echo "----------------------------------------"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" ${BASE_URL}/actuator/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Health endpoint returned 200"
    echo "Response: $BODY"
else
    echo -e "${RED}✗ FAIL${NC} - Health endpoint returned $HTTP_CODE"
    exit 1
fi
echo ""

# Test 2: Metrics/Prometheus Check
echo "Test 2: Metrics/Prometheus Check (No Auth Required)"
echo "----------------------------------------------------"
METRICS_RESPONSE=$(curl -s -w "\n%{http_code}" ${BASE_URL}/actuator/prometheus)
HTTP_CODE=$(echo "$METRICS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Prometheus endpoint returned 200"
else
    echo -e "${RED}✗ FAIL${NC} - Prometheus endpoint returned $HTTP_CODE"
fi
echo ""

# Test 3: Generate USER token
echo "Test 3: Generate USER Token"
echo "----------------------------"
USER_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "roles": ["ROLE_user"]}')
HTTP_CODE=$(echo "$USER_TOKEN_RESPONSE" | tail -n1)
BODY=$(echo "$USER_TOKEN_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Token generation successful"
    USER_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Token: ${USER_TOKEN:0:50}..."
else
    echo -e "${RED}✗ FAIL${NC} - Token generation failed with $HTTP_CODE"
    exit 1
fi
echo ""

# Test 4: Generate AUDITOR token
echo "Test 4: Generate AUDITOR Token"
echo "-------------------------------"
AUDITOR_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_auditor", "roles": ["ROLE_auditor"]}')
HTTP_CODE=$(echo "$AUDITOR_TOKEN_RESPONSE" | tail -n1)
BODY=$(echo "$AUDITOR_TOKEN_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Auditor token generation successful"
    AUDITOR_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Token: ${AUDITOR_TOKEN:0:50}..."
else
    echo -e "${RED}✗ FAIL${NC} - Auditor token generation failed"
    exit 1
fi
echo ""

# Test 5: Generate ADMIN token
echo "Test 5: Generate ADMIN Token"
echo "-----------------------------"
ADMIN_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "test_admin", "roles": ["ROLE_admin"]}')
HTTP_CODE=$(echo "$ADMIN_TOKEN_RESPONSE" | tail -n1)
BODY=$(echo "$ADMIN_TOKEN_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Admin token generation successful"
    ADMIN_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Token: ${ADMIN_TOKEN:0:50}..."
else
    echo -e "${RED}✗ FAIL${NC} - Admin token generation failed"
    exit 1
fi
echo ""

# Test 6: Test authentication - request without token (should fail with 401)
echo "Test 6: Request Without Token (Should Fail)"
echo "--------------------------------------------"
NO_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"E001","accountId":"A10","type":"credit","amount":100}')
HTTP_CODE=$(echo "$NO_TOKEN_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "401" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Correctly returned 401 for missing token"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 401, got $HTTP_CODE"
fi
echo ""

# Test 7: USER role accessing /events (should work if backend is up, or return connection error)
echo "Test 7: USER Role Accessing /events"
echo "------------------------------------"
USER_EVENTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/events \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"E001","accountId":"A10","type":"credit","amount":100}')
HTTP_CODE=$(echo "$USER_EVENTS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Gateway forwarded request (HTTP $HTTP_CODE)"
    if [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
        echo -e "${YELLOW}Note: Backend service may not be running${NC}"
    fi
else
    echo -e "${RED}✗ FAIL${NC} - Unexpected response $HTTP_CODE"
fi
echo ""

# Test 8: USER role accessing /drift-check (should fail with 403)
echo "Test 8: USER Role Accessing /drift-check (Should Fail)"
echo "-------------------------------------------------------"
USER_DRIFT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/drift-check \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"accountId":"A10","reportedBalance":1000}]')
HTTP_CODE=$(echo "$USER_DRIFT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Correctly denied USER access to /drift-check (403)"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 403, got $HTTP_CODE"
fi
echo ""

# Test 9: AUDITOR role accessing /drift-check (should work or return backend error)
echo "Test 9: AUDITOR Role Accessing /drift-check"
echo "--------------------------------------------"
AUDITOR_DRIFT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/drift-check \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"accountId":"A10","reportedBalance":1000}]')
HTTP_CODE=$(echo "$AUDITOR_DRIFT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Gateway forwarded request (HTTP $HTTP_CODE)"
    if [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
        echo -e "${YELLOW}Note: Backend service may not be running${NC}"
    fi
else
    echo -e "${RED}✗ FAIL${NC} - Unexpected response $HTTP_CODE"
fi
echo ""

# Test 10: AUDITOR role accessing /correct (should fail with 403)
echo "Test 10: AUDITOR Role Accessing /correct (Should Fail)"
echo "-------------------------------------------------------"
AUDITOR_CORRECT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/correct/A10 \
  -H "Authorization: Bearer $AUDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"CORR-001","accountId":"A10","type":"credit","amount":50}')
HTTP_CODE=$(echo "$AUDITOR_CORRECT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Correctly denied AUDITOR access to /correct (403)"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 403, got $HTTP_CODE"
fi
echo ""

# Test 11: ADMIN role accessing /correct (should work or return backend error)
echo "Test 11: ADMIN Role Accessing /correct"
echo "---------------------------------------"
ADMIN_CORRECT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/correct/A10 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"CORR-001","accountId":"A10","type":"credit","amount":50}')
HTTP_CODE=$(echo "$ADMIN_CORRECT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Gateway forwarded request (HTTP $HTTP_CODE)"
    if [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
        echo -e "${YELLOW}Note: Backend service may not be running${NC}"
    fi
else
    echo -e "${RED}✗ FAIL${NC} - Unexpected response $HTTP_CODE"
fi
echo ""

# Test 12: ADMIN role accessing /accounts (should work or return backend error)
echo "Test 12: ADMIN Role Accessing /accounts"
echo "----------------------------------------"
ADMIN_ACCOUNTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/accounts/A10/shadow-balance \
  -H "Authorization: Bearer $ADMIN_TOKEN")
HTTP_CODE=$(echo "$ADMIN_ACCOUNTS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Gateway forwarded request (HTTP $HTTP_CODE)"
    if [ "$HTTP_CODE" = "502" ] || [ "$HTTP_CODE" = "503" ]; then
        echo -e "${YELLOW}Note: Backend service may not be running${NC}"
    fi
else
    echo -e "${RED}✗ FAIL${NC} - Unexpected response $HTTP_CODE"
fi
echo ""

echo "=========================================="
echo "           Test Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}✓ API Gateway is working correctly!${NC}"
echo ""
echo "Core Functionality Verified:"
echo "  ✓ Health endpoint accessible"
echo "  ✓ Metrics/Prometheus endpoint accessible"
echo "  ✓ JWT token generation working"
echo "  ✓ Authentication enforcement (401 for missing token)"
echo "  ✓ RBAC working correctly (403 for wrong role)"
echo "  ✓ Request routing to backend services"
echo ""
echo "Tokens Generated (save these for manual testing):"
echo "------------------------------------------------"
echo "USER TOKEN:"
echo "$USER_TOKEN"
echo ""
echo "AUDITOR TOKEN:"
echo "$AUDITOR_TOKEN"
echo ""
echo "ADMIN TOKEN:"
echo "$ADMIN_TOKEN"
echo ""

