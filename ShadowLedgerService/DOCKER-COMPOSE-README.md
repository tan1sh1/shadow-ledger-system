# Shadow Ledger Service - Docker Compose Setup

## Overview
This Docker Compose configuration sets up the complete Shadow Ledger Service environment with all dependencies.

## Services Included

1. **PostgreSQL** - Database for storing ledger entries
2. **Zookeeper** - Coordination service for Kafka
3. **Kafka** - Message broker for transaction events
4. **Kafka Init** - Automatically creates required topics
5. **Shadow Ledger Service** - Main application
6. **Kafka UI** (Optional) - Web interface for monitoring Kafka

## Prerequisites

- Docker Desktop installed (with Docker Compose)
- At least 4GB RAM available for Docker
- Ports available: 5432, 2181, 9092, 29092, 8082, 8080

## Quick Start

### 1. Start All Services

```bash
docker-compose up -d
```

### 2. Check Service Status

```bash
docker-compose ps
```

### 3. View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f shadow-ledger-service
docker-compose logs -f kafka
```

### 4. Stop All Services

```bash
docker-compose down
```

### 5. Stop and Remove All Data

```bash
docker-compose down -v
```

## Service Endpoints

| Service | Endpoint | Description |
|---------|----------|-------------|
| Shadow Ledger API | http://localhost:8082 | Main REST API |
| Health Check | http://localhost:8082/actuator/health | Service health status |
| Metrics | http://localhost:8082/actuator/metrics | Application metrics |
| Shadow Balance | GET http://localhost:8082/accounts/{accountId}/shadow-balance | Get account balance |
| Kafka UI | http://localhost:8080 | Kafka monitoring dashboard |
| PostgreSQL | localhost:5432 | Database (user: postgres, pass: postgres, db: shadowledger) |

## Testing the Service

### 1. Check Service Health

```bash
curl http://localhost:8082/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

### 2. Produce Test Events to Kafka

```bash
# Credit Event
docker exec -it shadowledger-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic transactions.raw \
  --property "parse.key=true" \
  --property "key.separator=:"

# Then paste this (accountId:event):
A10:{"eventId":"E001","accountId":"A10","type":"CREDIT","amount":1000.00,"timestamp":"2025-12-14T10:00:00Z"}
A10:{"eventId":"E002","accountId":"A10","type":"DEBIT","amount":250.00,"timestamp":"2025-12-14T10:01:00Z"}
```

Press Ctrl+C to exit.

### 3. Query Shadow Balance

```bash
curl http://localhost:8082/accounts/A10/shadow-balance
```

Expected response:
```json
{
  "accountId": "A10",
  "balance": 750.00,
  "lastEvent": "E002"
}
```

### 4. View Kafka Topics

```bash
docker exec -it shadowledger-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

### 5. Consume Messages from Kafka

```bash
docker exec -it shadowledger-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transactions.raw \
  --from-beginning
```

## Database Access

### Connect to PostgreSQL

```bash
docker exec -it shadowledger-postgres psql -U postgres -d shadowledger
```

### Useful SQL Queries

```sql
-- View all ledger entries
SELECT * FROM ledger_entries ORDER BY timestamp, event_id;

-- Check balance for an account
SELECT account_id, 
       SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END) as balance
FROM ledger_entries
WHERE account_id = 'A10'
GROUP BY account_id;

-- View entries with running balance
WITH ordered_transactions AS (
    SELECT 
        event_id,
        account_id,
        type,
        amount,
        timestamp,
        CASE 
            WHEN type = 'CREDIT' THEN amount 
            WHEN type = 'DEBIT' THEN -amount 
        END as transaction_amount
    FROM ledger_entries
    WHERE account_id = 'A10'
    ORDER BY timestamp, event_id
)
SELECT 
    event_id,
    type,
    amount,
    timestamp,
    SUM(transaction_amount) OVER (
        ORDER BY timestamp, event_id 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as running_balance
FROM ordered_transactions;
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker-compose logs shadow-ledger-service

# Restart specific service
docker-compose restart shadow-ledger-service
```

### Kafka Connection Issues

```bash
# Check Kafka health
docker exec -it shadowledger-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# Recreate topics
docker-compose restart kafka-init
```

### Database Connection Issues

```bash
# Check PostgreSQL status
docker exec -it shadowledger-postgres pg_isready -U postgres

# View database logs
docker-compose logs postgres
```

### Reset Everything

```bash
# Stop all services and remove volumes
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

## Performance Testing

### Test with 1000 Events

Create a test script `test-events.sh`:

```bash
#!/bin/bash
for i in {1..1000}; do
  EVENT_ID="E$(printf "%05d" $i)"
  TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  
  if [ $((i % 2)) -eq 0 ]; then
    TYPE="CREDIT"
  else
    TYPE="DEBIT"
  fi
  
  AMOUNT=$((RANDOM % 500 + 100))
  
  echo "A10:{\"eventId\":\"$EVENT_ID\",\"accountId\":\"A10\",\"type\":\"$TYPE\",\"amount\":$AMOUNT.00,\"timestamp\":\"$TIMESTAMP\"}"
done | docker exec -i shadowledger-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic transactions.raw \
  --property "parse.key=true" \
  --property "key.separator=:"
```

Run it:
```bash
chmod +x test-events.sh
./test-events.sh
```

## Monitoring

### View Kafka UI
Open http://localhost:8080 in your browser to:
- View topics and messages
- Monitor consumer groups
- Check partition assignments
- View broker metrics

### Application Metrics

```bash
# View all available metrics
curl http://localhost:8082/actuator/metrics

# View specific metric (e.g., JVM memory)
curl http://localhost:8082/actuator/metrics/jvm.memory.used
```

## Environment Variables

You can override environment variables in `docker-compose.yml`:

```yaml
shadow-ledger-service:
  environment:
    DB_HOST: postgres
    DB_PORT: 5432
    DB_NAME: shadowledger
    DB_USER: postgres
    DB_PASSWORD: postgres
    KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    SPRING_PROFILES_ACTIVE: docker
```

## Network

All services run on the `banking-network` bridge network, allowing them to communicate using service names as hostnames.

## Data Persistence

- PostgreSQL data is persisted in a Docker volume named `postgres_data`
- Kafka data is ephemeral (lost on container restart)
- To preserve Kafka data, add a volume for Kafka in docker-compose.yml

## Scaling

To scale the Shadow Ledger Service:

```bash
docker-compose up -d --scale shadow-ledger-service=3
```

Note: You'll need to configure load balancing separately.

## Production Considerations

For production deployment:
1. Use external PostgreSQL (RDS)
2. Use managed Kafka (MSK)
3. Add proper secrets management
4. Configure resource limits
5. Set up monitoring and alerting
6. Enable SSL/TLS for all connections
7. Use proper authentication

