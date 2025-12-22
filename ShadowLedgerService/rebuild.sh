#!/bin/bash

echo "======================================"
echo "Rebuilding Shadow Ledger Service"
echo "======================================"

# Stop the service
echo -e "\n1. Stopping shadow-ledger-service..."
docker-compose stop shadow-ledger-service

# Rebuild the service
echo -e "\n2. Rebuilding shadow-ledger-service..."
docker-compose build shadow-ledger-service

# Start the service
echo -e "\n3. Starting shadow-ledger-service..."
docker-compose up -d shadow-ledger-service

# Wait for service to be ready
echo -e "\n4. Waiting for service to be ready..."
sleep 10

# Check status
echo -e "\n5. Service Status:"
docker-compose ps shadow-ledger-service

# Check health
echo -e "\n6. Health Check:"
curl -s http://localhost:8082/actuator/health | python3 -m json.tool

echo -e "\n======================================"
echo "✅ Rebuild Complete!"
echo "======================================"

