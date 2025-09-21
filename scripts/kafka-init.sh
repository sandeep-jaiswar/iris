#!/bin/bash

# Kafka Initialization Script
# This script formats Kafka storage and creates default topics

set -e

KAFKA_DIR="/opt/kafka"
STORAGE_DIR="/var/lib/kafka"
BOOTSTRAP_SERVER="kafka:9092"

echo "Starting Kafka initialization..."

# Function to check if Kafka is ready
wait_for_kafka() {
    echo "Waiting for Kafka to be ready..."
    local retries=30
    local count=0
    
    while [ $count -lt $retries ]; do
        if $KAFKA_DIR/bin/kafka-broker-api-versions.sh --bootstrap-server $BOOTSTRAP_SERVER >/dev/null 2>&1; then
            echo "Kafka is ready!"
            return 0
        fi
        echo "Kafka not ready yet. Waiting... ($((count+1))/$retries)"
        sleep 2
        count=$((count+1))
    done
    
    echo "ERROR: Kafka failed to start within expected time"
    return 1
}

# Function to create a topic if it doesn't exist
create_topic() {
    local topic_name="$1"
    local partitions="${2:-3}"
    local replication_factor="${3:-1}"
    
    echo "Creating topic: $topic_name (partitions: $partitions, replication-factor: $replication_factor)"
    
    $KAFKA_DIR/bin/kafka-topics.sh \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --create \
        --if-not-exists \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication_factor
    
    if [ $? -eq 0 ]; then
        echo "Topic '$topic_name' created successfully or already exists"
    else
        echo "ERROR: Failed to create topic '$topic_name'"
        return 1
    fi
}

# Function to list all topics
list_topics() {
    echo "Listing all topics:"
    $KAFKA_DIR/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list
}

# Function to describe topics
describe_topics() {
    echo "Describing topics:"
    $KAFKA_DIR/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --describe
}

# Main initialization process
main() {
    echo "========================================"
    echo "Kafka KRaft Initialization Script"
    echo "========================================"
    
    # Wait for Kafka to be ready
    wait_for_kafka
    
    echo "Creating default topics..."
    
    # Create default topics for the application
    create_topic "test-topic" 3 1
    create_topic "user-events" 3 1
    create_topic "system-logs" 3 1
    create_topic "notifications" 2 1
    create_topic "metrics" 1 1
    
    # IRIS-specific topics for financial data
    create_topic "trade-events" 6 1
    create_topic "market-data" 6 1
    create_topic "fx-rates" 3 1
    
    # Additional topics for development/testing
    create_topic "dev-topic" 1 1
    create_topic "integration-test" 1 1
    
    echo ""
    echo "Topic creation completed!"
    echo ""
    
    # List and describe topics for verification
    list_topics
    echo ""
    describe_topics
    
    echo "========================================"
    echo "Kafka initialization completed successfully!"
    echo "Kafka is accessible at: $BOOTSTRAP_SERVER"
    echo "========================================"
}

# Script execution based on arguments
case "${1:-init}" in
    "init"|"")
        main
        ;;
    "create-topic")
        if [ -z "$2" ]; then
            echo "Usage: $0 create-topic <topic-name> [partitions] [replication-factor]"
            exit 1
        fi
        wait_for_kafka
        create_topic "$2" "$3" "$4"
        ;;
    "list-topics")
        wait_for_kafka
        list_topics
        ;;
    "describe-topics")
        wait_for_kafka
        describe_topics
        ;;
    "help")
        echo "Kafka Init Script Commands:"
        echo "  init                                 - Initialize Kafka and create default topics"
        echo "  create-topic <name> [parts] [rf]     - Create a single topic"
        echo "  list-topics                          - List all topics"
        echo "  describe-topics                      - Describe all topics"
        echo "  help                                 - Show this help message"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for available commands"
        exit 1
        ;;
esac