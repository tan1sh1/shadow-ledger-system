#!/bin/bash

# API Gateway Quick Start Script
# This script helps you get the API Gateway up and running quickly

set -e

echo "╔══════════════════════════════════════════════════════╗"
echo "║   API Gateway - Quick Start Setup                   ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# Function to print colored output
print_success() {
    echo "✅ $1"
}

print_info() {
    echo "ℹ️  $1"
}

print_error() {
    echo "❌ $1"
}

# Check if Docker is running
check_docker() {
    print_info "Checking Docker installation..."
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi

    print_success "Docker is running"
}

# Build the project
build_project() {
    print_info "Building the API Gateway project..."
    chmod +x gradlew
    ./gradlew clean build -x test

    if [ $? -eq 0 ]; then
        print_success "Build completed successfully!"
    else
        print_error "Build failed. Please check the errors above."
        exit 1
    fi
}

# Show menu
show_menu() {
    echo ""
    echo "Choose an option:"
    echo "1) Build project only"
    echo "2) Run API Gateway locally (standalone)"
    echo "3) Run all services with Docker Compose"
    echo "4) Generate sample JWT tokens"
    echo "5) Exit"
    echo ""
    read -p "Enter your choice [1-5]: " choice

    case $choice in
        1)
            build_project
            show_menu
            ;;
        2)
            run_local
            ;;
        3)
            run_docker_compose
            ;;
        4)
            generate_tokens
            ;;
        5)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            print_error "Invalid option. Please try again."
            show_menu
            ;;
    esac
}

# Run locally
run_local() {
    print_info "Starting API Gateway locally on port 8080..."
    print_info "Note: Backend services must be running separately"
    echo ""
    ./gradlew bootRun
}

# Run with Docker Compose
run_docker_compose() {
    check_docker
    print_info "Starting all services with Docker Compose..."
    print_info "This will start: API Gateway, Event Service, Shadow Ledger, Drift/Correction, Kafka, PostgreSQL"
    echo ""
    docker-compose up --build
}

# Generate sample tokens
generate_tokens() {
    print_info "To generate JWT tokens, first ensure the API Gateway is running."
    print_info "Then use these curl commands:"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "USER TOKEN (can access /events):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat << 'EOF'
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_user",
    "roles": ["ROLE_user"]
  }'
EOF
    echo ""
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "AUDITOR TOKEN (can access /drift-check, /accounts):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat << 'EOF'
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_auditor",
    "roles": ["ROLE_auditor"]
  }'
EOF
    echo ""
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "ADMIN TOKEN (can access /correct/{id} + all above):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat << 'EOF'
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin_user",
    "roles": ["ROLE_admin"]
  }'
EOF
    echo ""
    echo ""
    print_info "After getting a token, test with:"
    echo ""
    cat << 'EOF'
export TOKEN="<your-token-here>"

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
EOF
    echo ""
    show_menu
}

# Main execution
echo "Welcome to the API Gateway Quick Start!"
echo ""
show_menu

