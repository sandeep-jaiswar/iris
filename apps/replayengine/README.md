# IRIS Replay Engine

The IRIS Replay Engine is a high-performance, production-ready service for replaying financial data from Chipmunk files into Kafka topics.

## Features

- **Multi-Source Support**: Read Chipmunk files from MinIO/S3 or local filesystem
- **Configurable Replay Modes**: 
  - Real-time: Maintains original timing between events
  - Accelerated: Replay at configurable speed multiplier
  - Burst: Maximum speed replay with batching
- **Crash Recovery**: Automatic checkpointing with configurable intervals
- **High Performance**: Low-latency Kafka publishing with metrics
- **Production Ready**: Prometheus metrics, health checks, REST API
- **Event Types**: Supports trade events, market data, and FX rates

## Quick Start

### Prerequisites

- Java 17+
- Kafka cluster (localhost:9092 by default)
- MinIO/S3 for file storage (or local files)

### Configuration

Copy and customize `replay-config.yaml`:

```yaml
replay:
  speed-mode: real-time  # real-time, accelerated, burst
  speed-multiplier: 10.0  # for accelerated mode
  burst-batch-size: 1000  # for burst mode
  
  source:
    type: minio  # minio, local-file
    minio:
      bucket-name: iris-chipmunk-files
      object-key: sample-data/trades-2024-01-01.chipmunk

kafka:
  bootstrap-servers: localhost:9092
  topics:
    trade-events: trade-events
    market-data: market-data
    fx-rates: fx-rates

aws:
  endpoint-url: http://localhost:4566  # MinIO/LocalStack
  access-key: test
  secret-key: test
```

### Running the Application

```bash
# Start the replay engine
./gradlew apps:replayengine:bootRun

# Or use the built JAR
java -jar apps/replayengine/build/libs/replayengine.jar
```

The application will start on port 8081 by default.

## REST API

### Start Replay
```bash
POST http://localhost:8081/api/replay/start
```

### Stop Replay
```bash
POST http://localhost:8081/api/replay/stop
```

### Check Status
```bash
GET http://localhost:8081/api/replay/status
```

### Health Check
```bash
GET http://localhost:8081/api/replay/health
```

## Metrics

Prometheus metrics are available at:
```
http://localhost:8081/actuator/prometheus
```

Key metrics:
- `replay_events_processed_total`: Number of events processed
- `replay_events_failed_total`: Number of failed events
- `replay_events_publish_latency_seconds`: Kafka publish latency
- `replay_is_running`: Whether replay is currently active (1=running, 0=stopped)

## Chipmunk File Format

The engine expects JSON events, one per line:

```json
{"timestamp": "2024-01-01T10:00:00Z", "trade_id": "T001", "symbol": "AAPL", "price": 150.0, "quantity": 100, "region": "US"}
{"timestamp": "2024-01-01T10:00:01Z", "symbol": "AAPL", "price": 150.1, "bid": 149.9, "ask": 150.2, "region": "US"}
{"timestamp": "2024-01-01T10:00:02Z", "base_currency": "USD", "target_currency": "EUR", "rate": 0.85, "region": "US"}
```

Events are automatically routed to the correct Kafka topic based on content:
- Trade events → `trade-events` topic
- Market data → `market-data` topic  
- FX rates → `fx-rates` topic

## Architecture

```
┌─────────────────┐    ┌───────────────────┐    ┌─────────────────┐
│   MinIO/S3      │    │  Replay Engine    │    │     Kafka       │
│                 │────▶│                   │────▶│                 │
│ Chipmunk Files  │    │ • ChipmunkReader  │    │ • trade-events  │
└─────────────────┘    │ • ReplayScheduler │    │ • market-data   │
                       │ • KafkaPublisher  │    │ • fx-rates      │
                       │ • CheckpointMgr   │    └─────────────────┘
                       │ • MetricsCollector│
                       └───────────────────┘
                               │
                       ┌───────────────────┐
                       │   Prometheus      │
                       │     Metrics       │
                       └───────────────────┘
```

## Development

### Building
```bash
./gradlew apps:replayengine:build
```

### Testing
```bash
./gradlew apps:replayengine:test
```

### Running Locally
```bash
# Start infrastructure
docker compose up -d

# Create topics (if not already created)
./scripts/kafka-init.sh

# Start the application
./gradlew apps:replayengine:bootRun
```

## Production Deployment

1. **Kafka Topics**: Ensure the required topics exist with appropriate partitions:
   ```bash
   kafka-topics.sh --create --topic trade-events --partitions 6 --replication-factor 3
   kafka-topics.sh --create --topic market-data --partitions 6 --replication-factor 3
   kafka-topics.sh --create --topic fx-rates --partitions 3 --replication-factor 3
   ```

2. **Configuration**: Use production-specific configuration:
   - Real Kafka cluster endpoints
   - Production MinIO/S3 credentials  
   - Appropriate checkpoint intervals
   - Resource limits and JVM tuning

3. **Monitoring**: Set up alerts on key metrics:
   - High failure rates
   - Publish latency spikes
   - Checkpoint failures

4. **Scaling**: Deploy multiple instances for high availability:
   - Use different file sources per instance
   - Ensure checkpoint storage is instance-specific
   - Monitor resource usage and tune accordingly

## Troubleshooting

### Common Issues

1. **Connection refused to Kafka**: Check Kafka is running and accessible
2. **MinIO access denied**: Verify AWS credentials and bucket permissions
3. **High memory usage**: Adjust JVM heap size and consider smaller batch sizes
4. **Slow replay**: Check Kafka producer configuration and network latency

### Logs

Enable debug logging for troubleshooting:
```yaml
logging:
  level:
    org.jaiswarsecurities.replayengine: DEBUG
    org.apache.kafka: INFO
```

### Health Checks

The application provides multiple health endpoints:
- `/api/replay/health` - Basic health status
- `/actuator/health` - Spring Boot health with dependencies
- `/actuator/metrics` - All available metrics