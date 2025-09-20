# Iris

A Java multi-module project with integrated Kafka development infrastructure.

## Overview

This project provides a complete development environment with:
- Java application modules (app, list, utilities)
- Kafka 4.1.0 in KRaft mode (no Zookeeper required)
- Kafka-UI for topic monitoring and management
- Docker Compose orchestration for easy setup

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Java 17+ (for building the Java modules)
- Gradle (wrapper included)

### Running Kafka Infrastructure

1. **Start Kafka and Kafka-UI:**
   ```bash
   docker compose up -d
   ```

2. **Verify services are running:**
   ```bash
   docker compose ps
   ```

3. **Access Kafka-UI:**
   Open your browser and go to [http://localhost:8080](http://localhost:8080)

4. **Access Kafka directly:**
   Kafka is available at `localhost:9092`

### Building the Java Application

```bash
./gradlew build
```

### Running the Java Application

```bash
./gradlew :app:run
```

## Kafka Infrastructure

### Services

- **Kafka Broker**: Runs on `localhost:9092` in KRaft mode
- **Kafka-UI**: Web interface available at `localhost:8080`

### Default Topics

The following topics are automatically created on startup:

| Topic Name | Partitions | Replication Factor | Purpose |
|------------|------------|-------------------|---------|
| `test-topic` | 3 | 1 | General testing |
| `user-events` | 3 | 1 | User activity events |
| `system-logs` | 3 | 1 | Application logs |
| `notifications` | 2 | 1 | User notifications |
| `metrics` | 1 | 1 | System metrics |
| `dev-topic` | 1 | 1 | Development testing |
| `integration-test` | 1 | 1 | Integration tests |

### Data Persistence

Kafka data is persisted using Docker volumes:
- `kafka-data`: Stores Kafka log segments
- `kafka-metadata`: Stores KRaft metadata

Data persists across container restarts and system reboots.

## Managing Kafka

### Using the Init Script

The project includes a helper script for Kafka management:

```bash
# Initialize Kafka and create default topics (automatically done on startup)
./scripts/kafka-init.sh init

# Create a custom topic
./scripts/kafka-init.sh create-topic my-custom-topic 5 1

# List all topics
./scripts/kafka-init.sh list-topics

# Describe topics (shows partitions, replicas, etc.)
./scripts/kafka-init.sh describe-topics

# Show help
./scripts/kafka-init.sh help
```

### Manual Kafka Operations

You can also interact with Kafka directly using Docker:

```bash
# List topics
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create a topic
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic my-topic --partitions 3 --replication-factor 1

# Produce messages to a topic
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic

# Consume messages from a topic
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

## Adding Additional Topics

### Method 1: Using the Init Script

```bash
./scripts/kafka-init.sh create-topic new-topic-name 3 1
```

### Method 2: Modifying docker-compose.yml

Add topic creation commands to the Kafka service's startup script in `docker-compose.yml`:

```yaml
# In the kafka service command section, add:
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic new-topic --partitions 3 --replication-factor 1
```

### Method 3: Auto-creation

Kafka is configured with `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"`, so topics will be automatically created when first accessed by producers or consumers.

## Scaling Brokers

To add more Kafka brokers for a multi-broker setup:

1. **Add additional broker services in docker-compose.yml:**

```yaml
  kafka-2:
    image: apache/kafka:4.1.0
    container_name: kafka-2
    hostname: kafka-2
    ports:
      - "9094:9092"
      - "9095:9093"
    environment:
      KAFKA_NODE_ID: 2
      KAFKA_PROCESS_ROLES: broker
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      # ... other configuration
    volumes:
      - kafka-2-data:/var/lib/kafka/data
    depends_on:
      kafka:
        condition: service_healthy
```

2. **Update replication factors** for better fault tolerance:
   - Change `KAFKA_DEFAULT_REPLICATION_FACTOR` to 2 or 3
   - Recreate topics with higher replication factors

## Troubleshooting

### Common Issues

1. **Port conflicts**: Make sure ports 8080, 9092, and 9093 are not in use
2. **Memory issues**: Adjust `KAFKA_HEAP_OPTS` in docker-compose.yml if needed
3. **Startup timing**: Kafka-UI depends on Kafka being healthy; wait for full startup

### Useful Commands

```bash
# Check logs
docker compose logs kafka
docker compose logs kafka-ui

# Restart services
docker compose restart kafka kafka-ui

# Clean reset (removes all data)
docker compose down -v
docker compose up -d

# Check Kafka health
docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Monitoring

- **Kafka-UI Dashboard**: Real-time monitoring of topics, consumers, and brokers
- **Docker Stats**: `docker stats` shows resource usage
- **Kafka Logs**: Available through `docker compose logs kafka`

## Development Workflow

1. Start the infrastructure: `docker compose up -d`
2. Develop your Java application using Kafka at `localhost:9092`
3. Monitor topics and messages using Kafka-UI at `localhost:8080`
4. Test with different topics and consumer groups
5. Scale or modify configuration as needed

## Configuration

### Environment Variables

Key Kafka configuration can be modified via environment variables in `docker-compose.yml`:

- `KAFKA_NUM_PARTITIONS`: Default partition count for auto-created topics
- `KAFKA_DEFAULT_REPLICATION_FACTOR`: Default replication factor
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE`: Enable/disable auto topic creation
- `KAFKA_HEAP_OPTS`: JVM memory settings

### Network Access

- **Internal**: Services communicate using container names (`kafka`, `kafka-ui`)
- **External**: Access via `localhost:9092` (Kafka) and `localhost:8080` (Kafka-UI)

## Project Structure

```
iris/
├── app/                    # Main Java application
├── list/                   # Linked list utility module
├── utilities/              # String utilities module
├── scripts/
│   └── kafka-init.sh      # Kafka initialization script
├── docker-compose.yml     # Docker Compose configuration
├── gradle/                # Gradle wrapper files
├── gradlew               # Gradle wrapper script
└── README.md             # This file
```

## License

[Add your license information here]
