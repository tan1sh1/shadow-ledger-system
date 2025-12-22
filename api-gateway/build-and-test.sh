#!/bin/bash

echo "========================================="
echo "API Gateway - Build and Test Script"
echo "========================================="
echo ""

# Build the project
echo "Step 1: Building the API Gateway..."
./gradlew clean build -x test

if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed! Please check the errors above."
    exit 1
fi

echo ""
echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo ""
echo "To run the application:"
echo "  ./gradlew bootRun"
echo ""
echo "Or with Docker:"
echo "  docker build -t api-gateway ."
echo "  docker run -p 8080:8080 api-gateway"
echo ""
echo "Or with Docker Compose (all services):"
echo "  docker-compose up --build"
echo ""

