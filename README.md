# Distributed Inventory Management System (Local Dev)
[PT-BR]



This project demonstrates a distributed inventory management system with:
- Backend: Spring Boot 3 (Java 17)
- Frontend: React 18
- Event-driven architecture: Kafka (local Redpanda)
- API Gateway: Spring Cloud Gateway + Redis rate limiting
- Database (dev): H2 in-memory with auto seeding at startup

All backend APIs are prefixed with /api to comply with ingress rules.

## Quick Start (Local)

Prerequisites:
- Java 17 (JDK)
- Maven 3.9+
- Node.js 18+ and Yarn
- Redis (optional for dev rate limit) or use docker `redis:alpine`
- Redpanda (Kafka-compatible). You can run via docker or use the included supervisor config.

### 1) Start Redpanda (Kafka)
Option A – Docker:
```
docker run -d --pull=always --name=redpanda -p 9092:9092 -p 9644:9644 \
  docker.redpanda.com/redpandadata/redpanda:latest \
  redpanda start --overprovisioned --smp 1 --memory 512M \
  --reserve-memory 0M --node-id 0 --check=false \
  --kafka-addr 0.0.0.0:9092 --advertise-kafka-addr 127.0.0.1:9092 \
  --set redpanda.auto_create_topics_enabled=true
```

Option B – Local binary: install redpanda and run the same `redpanda start` flags.

### 2) Start Redis (optional for dev)
```
docker run -d --name redis -p 6379:6379 redis:alpine
```

### 3) Build & run backend
```
cd backend
mvn -DskipTests package
java -jar target/distributed-inventory-system-0.0.1-SNAPSHOT.jar
```
Backend binds to 0.0.0.0:8001, DB is H2 in-memory and seeds sample data every start.

### 4) Run API Gateway
```
cd ../api-gateway
mvn -DskipTests package
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```
Gateway runs on 8080 and proxies /api/* to backend.

### 5) Run frontend
```
cd ../frontend
yarn install
yarn start
```
Frontend uses REACT_APP_BACKEND_URL from frontend/.env. All API calls go to `${REACT_APP_BACKEND_URL}/api/*`.

## Architecture
- EDA: Kafka topics
  - inventory.events (3 partitions)
  - inventory.commands.stock (3 partitions)
  - transfers.commands (3 partitions)
  - notifications.events (1 partition)
  - inventory.dlq (1 partition)
- Producers: InventoryEventPublisher (KafkaTemplate)
- Consumers: @KafkaListener-based consumers for updates, transfers, audit
- Saga pattern: InventoryTransferSaga orchestrates RESERVE -> CONFIRM -> COMPLETE/ROLLBACK
- Caching: Redis via Spring Cache
- Circuit breaker/Retry: Resilience4j on service methods

## Developer Notes
- Dev DB: H2 in-memory with `ddl-auto: create-drop` and DataInitializer produces realistic data for stores, products, inventory, transactions.
- All serialization uses Jackson with JSR-310 module for dates.
- WebSocket endpoints broadcast notifications and inventory updates.

## Compliance Checklist vs. Requirements
- Java Spring Boot backend: YES
- React frontend with data visible: YES (inventory page, seeded on backend start)
- DB in dev in-memory with seed on start: YES (H2)
- Messaging: Kafka (Redpanda), producers/consumers/saga: YES
- API Gateway with Redis: YES
- Resilience/circuit breaker/retry: YES (Resilience4j)
- Strong consistency: optimistic versioning + transactional writes; saga for transfers
- Documentation: this README + topic diagram (see diagrams/)

## Diagrams
See `diagrams/eda-topics.png` and `diagrams/components.png` for message flow and components.

## Submission
- Include this repository with the full source.
- Provide a short demo: run Redpanda, Redis, backend, gateway, frontend; visit the Inventory page and confirm non-zero data and stock operations.
- Mention Java 17 + Spring Boot 3, Kafka (Redpanda), H2 in-memory, Redis, React.
