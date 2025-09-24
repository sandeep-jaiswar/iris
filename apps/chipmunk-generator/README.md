# IRIS Chipmunk File Generator

The IRIS Chipmunk File Generator creates production-ready chipmunk files containing realistic **Trade, Market Data, and FX events** in Protobuf format for testing and development.

## Features

- **Protobuf Event Schemas**: Uses canonical Protobuf format for all financial events
- **Realistic Data Generation**: Creates production-like mock data with proper distributions
- **Multi-Region Support**: Generates events for UK, US, Japan, and China regions
- **MinIO Upload**: Automatically uploads generated files to MinIO/S3 storage
- **Configurable Ratios**: Customizable mix of event types and time distributions
- **CLI Interface**: Simple command-line interface for easy usage

## Quick Start

### Prerequisites

- Java 17+
- MinIO/LocalStack running (for uploads)
- Docker Compose environment (optional)

### Running the Generator

```bash
# Generate a sample file with default settings (10,000 events)
./gradlew apps:chipmunk-generator:bootRun

# Generate a custom file with specific parameters
./gradlew apps:chipmunk-generator:bootRun --args="my-events.chip 5000"

# Generate using Docker Compose environment
docker compose up -d  # Start MinIO
./gradlew apps:chipmunk-generator:bootRun
```

### Command Line Arguments

1. **fileName** (optional): Name of the chipmunk file to generate
   - Default: `sample-events-{timestamp}.chip`
   
2. **eventCount** (optional): Number of events to generate
   - Default: `10000`

### Example Usage

```bash
# Generate 50,000 events in a file named "trading-day-2024.chip"
./gradlew apps:chipmunk-generator:bootRun --args="trading-day-2024.chip 50000"
```

## Configuration

Configure the generator via `application.yml`:

```yaml
chipmunk:
  generator:
    bucket-name: chipmunk-archive
    base-path: generated
    regions: [UK, US, JP, CN]
    event-generation:
      trade-event-ratio: 0.5        # 50% trade events
      market-data-event-ratio: 0.3  # 30% market data
      fx-rate-event-ratio: 0.2      # 20% FX events
      time-range-hours: 24          # Spread events over 24 hours
      instruments: [AAPL, MSFT, GOOGL, ...]
      currency-pairs: [USD/EUR, USD/GBP, ...]
      venues: [LSE, NYSE, NASDAQ, ...]

aws:
  endpoint-url: http://localhost:4566  # MinIO/LocalStack endpoint
  access-key: test
  secret-key: test
```

## Event Schemas

The generator creates events using the following Protobuf schemas:

### TradeEvent
```protobuf
message TradeEvent {
  string tradeId = 1;
  string orderId = 2;
  string instrument = 3;
  string account = 4;
  string counterparty = 5;
  double quantity = 6;
  double price = 7;
  string side = 8;              // BUY or SELL
  string venue = 9;
  int64 tradeTimestamp = 10;
  string status = 11;           // NEW, FILLED, PARTIALLY_FILLED
  string regulatoryId = 12;
  string traderId = 13;
  string strategyTag = 14;      // ALGO, MANUAL, HFT, etc.
  string correlationId = 15;
}
```

### MarketDataEvent
```protobuf
message MarketDataEvent {
  string instrument = 1;
  double bid = 2;
  double ask = 3;
  double lastPrice = 4;
  double bidSize = 5;
  double askSize = 6;
  int64 timestamp = 7;
  string venue = 8;
  string sourceFeed = 9;        // Bloomberg, Reuters, etc.
}
```

### FxRateEvent
```protobuf
message FxRateEvent {
  string fromCurrency = 1;      // USD, EUR, GBP, etc.
  string toCurrency = 2;
  double rate = 3;
  double bid = 4;
  double ask = 5;
  int64 timestamp = 6;
  string source = 7;            // EBS, Refinitiv, etc.
}
```

## File Format

Generated `.chip` files contain:
- **ChipmunkRecord** wrapper for each event
- **Length-prefixed** binary format for efficient parsing
- **Chronologically sorted** events
- **Mixed event types** based on configured ratios

Each ChipmunkRecord contains:
- Event type (TRADE, MARKET_DATA, FX)
- Timestamp
- Region
- Correlation ID (for linking related events)
- Serialized Protobuf payload

## Integration with Replay Engine

Generated files are compatible with the IRIS Replay Engine:

1. **Upload Location**: Files are uploaded to `s3://chipmunk-archive/generated/`
2. **Consumption**: Replay Engine can read and replay the generated events
3. **Format Compatibility**: Uses the same ChipmunkEvent structure

Configure the Replay Engine to consume generated files:

```yaml
replay:
  source:
    type: minio
    minio:
      bucket-name: chipmunk-archive
      object-key: generated/your-file.chip
```

## Data Characteristics

The generator creates realistic financial data with:

### Trade Events
- Regional trade ID prefixes (LON-, NYC-, TKY-, SHG-)
- Currency-appropriate account IDs
- Realistic price ranges ($50-$1000)
- Round lot quantities (multiples of 100)
- Venue-specific counterparties
- Regional regulatory IDs

### Market Data
- Realistic bid-ask spreads (1-20 basis points)
- Market depth with appropriate sizes
- Venue-specific characteristics
- Multiple data feed sources

### FX Rates
- Realistic exchange rates with small variations
- Tight spreads (1-5 pips)
- Major currency pair coverage
- Multiple liquidity providers

## Development

### Building
```bash
./gradlew apps:chipmunk-generator:build
```

### Testing
```bash
./gradlew apps:chipmunk-generator:test
```

### Running Locally
```bash
# Start infrastructure
docker compose up -d

# Run the generator
./gradlew apps:chipmunk-generator:bootRun
```

## Monitoring

The application provides:
- **Logging**: Detailed generation progress and upload confirmation
- **Metrics**: Integration with Spring Boot Actuator
- **Health Checks**: `/actuator/health` endpoint

## Troubleshooting

### Common Issues

1. **MinIO Connection Failed**
   - Check `aws.endpoint-url` configuration
   - Verify MinIO is running: `docker compose ps`
   - Check credentials and bucket permissions

2. **Large Memory Usage**
   - Reduce event count for testing
   - Adjust JVM heap size: `-Xmx2g`

3. **Protobuf Compilation Issues**
   - Ensure `iris.proto` is present in root directory
   - Clean and rebuild: `./gradlew clean build`

### Logs

Enable debug logging for troubleshooting:
```yaml
logging:
  level:
    org.jaiswarsecurities.chipmunkgenerator: DEBUG
```

## Example Output

```
2024-01-01 10:00:00 INFO  Starting Chipmunk File Generator...
2024-01-01 10:00:00 INFO  Generating chipmunk file: sample-events-1704110400000.chip with 10000 events
2024-01-01 10:00:01 INFO  Starting generation of 10000 events for file: sample-events-1704110400000.chip
2024-01-01 10:00:02 INFO  Successfully generated and uploaded 10000 records (2.1MB) to s3://chipmunk-archive/generated/sample-events-1704110400000.chip
2024-01-01 10:00:02 INFO  Successfully generated and uploaded chipmunk file to: s3://chipmunk-archive/generated/sample-events-1704110400000.chip
2024-01-01 10:00:02 INFO  Chipmunk File Generator completed successfully!
```