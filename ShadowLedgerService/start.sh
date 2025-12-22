#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Shadow Ledger Service - Docker Startup ===${NC}\n"

# Navigate to project directory
cd /Users/TANISH.M/Downloads/ShadowLedgerService

# Stop any existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose down

# Build and start services
echo -e "${BLUE}Building and starting services...${NC}"
docker-compose up -d --build

# Wait a moment
sleep 5

# Check status
echo -e "\n${BLUE}Service Status:${NC}"
docker-compose ps

echo -e "\n${GREEN}=== Services Starting ===${NC}"
echo -e "Give it 30-60 seconds for all services to be healthy."
echo -e "\nTo check logs:"
echo -e "  ${BLUE}docker-compose logs -f shadow-ledger-service${NC}"
echo -e "\nTo check health:"
echo -e "  ${BLUE}curl http://localhost:8082/actuator/health${NC}"
echo -e "\n${GREEN}Service Endpoints:${NC}"
echo -e "  Shadow Ledger API: http://localhost:8082"
echo -e "  Kafka UI:          http://localhost:8080"
echo -e "  PostgreSQL:        localhost:5433"

