package org.jaiswarsecurities.replayengine.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify Kafka producer configuration compatibility.
 * This ensures that the configuration doesn't have conflicting settings.
 */
class KafkaConfigurationTest {

    @Test
    void testKafkaProducerConfigurationWithIdempotence() {
        // Simulate the same configuration used in ReplayEngineConfig
        Properties props = new Properties();
        
        // Basic configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // Performance and reliability configuration that matches our replay-config.yaml
        props.put(ProducerConfig.ACKS_CONFIG, "all");  // Changed from "1" to "all"
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432L);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        
        // Enable idempotence for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // This should not throw an exception now that acks is set to "all"
        assertDoesNotThrow(() -> {
            try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                // Producer creation should succeed without configuration errors
                assertNotNull(producer);
            }
        }, "KafkaProducer should be created successfully with idempotent configuration");
    }
    
    @Test
    void testKafkaProducerConfigurationFailsWithWrongAcks() {
        // Test that the old configuration (acks=1) would fail with idempotence enabled
        Properties props = new Properties();
        
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // Use the problematic configuration (acks=1 with idempotence=true)
        props.put(ProducerConfig.ACKS_CONFIG, "1");  // This should cause failure
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // This should throw a ConfigException
        assertThrows(Exception.class, () -> {
            new KafkaProducer<String, String>(props);
        }, "KafkaProducer should fail when acks=1 and idempotence=true");
    }
}