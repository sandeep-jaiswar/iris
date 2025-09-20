package org.jaiswarsecurities.replayengine.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jaiswarsecurities.replayengine.config.KafkaProperties;
import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Service for publishing events to Kafka topics.
 * Handles routing events to the correct topics and provides metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisher {
    
    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaProperties kafkaProperties;
    private final MeterRegistry meterRegistry;
    
    private Counter publishedEventsCounter;
    private Counter failedEventsCounter;
    private Timer publishLatencyTimer;
    
    @PostConstruct
    public void initMetrics() {
        publishedEventsCounter = Counter.builder("replay.events.published")
                .description("Number of events successfully published to Kafka")
                .tag("component", "kafka-publisher")
                .register(meterRegistry);
                
        failedEventsCounter = Counter.builder("replay.events.failed")
                .description("Number of events that failed to publish to Kafka")
                .tag("component", "kafka-publisher")
                .register(meterRegistry);
                
        publishLatencyTimer = Timer.builder("replay.events.publish.latency")
                .description("Time taken to publish events to Kafka")
                .tag("component", "kafka-publisher")
                .register(meterRegistry);
    }
    
    /**
     * Publishes an event to the appropriate Kafka topic based on its type.
     * 
     * @param event The event to publish
     * @return A future that completes when the event is published
     */
    public CompletableFuture<RecordMetadata> publishEvent(ChipmunkEvent event) {
        long startTime = System.nanoTime();
        
        String topicName = getTopicName(event.getEventType());
        String key = generateKey(event);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topicName,
                key,
                event.getJsonPayload()
        );
        
        // Add headers for tracing and metadata
        record.headers().add("event-id", event.getEventId().getBytes());
        record.headers().add("event-type", event.getEventType().name().getBytes());
        record.headers().add("region", event.getRegion().getBytes());
        record.headers().add("line-number", String.valueOf(event.getLineNumber()).getBytes());
        
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        kafkaProducer.send(record, (metadata, exception) -> {
            publishLatencyTimer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
            
            if (exception == null) {
                log.debug("Event published successfully: topic={}, partition={}, offset={}, eventId={}", 
                        metadata.topic(), metadata.partition(), metadata.offset(), event.getEventId());
                publishedEventsCounter.increment();
                future.complete(metadata);
            } else {
                log.error("Failed to publish event: eventId={}, topic={}", 
                        event.getEventId(), topicName, exception);
                failedEventsCounter.increment();
                future.completeExceptionally(exception);
            }
        });
        
        return future;
    }
    
    /**
     * Flushes any pending records and closes the producer.
     */
    public void close() {
        log.info("Closing Kafka publisher...");
        kafkaProducer.flush();
        kafkaProducer.close();
    }
    
    private String getTopicName(ChipmunkEvent.EventType eventType) {
        switch (eventType) {
            case TRADE:
                return kafkaProperties.getTopics().getTradeEvents();
            case MARKET_DATA:
                return kafkaProperties.getTopics().getMarketData();
            case FX_RATE:
                return kafkaProperties.getTopics().getFxRates();
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
    
    private String generateKey(ChipmunkEvent event) {
        // Generate a key for partitioning. In a real system, this would be based on
        // business logic like symbol, region, or trading account
        return event.getRegion() + "_" + event.getEventType().name();
    }
}