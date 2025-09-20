package org.jaiswarsecurities.replayengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.jaiswarsecurities.awsconfig.AwsConfig;

import java.util.Properties;

/**
 * Main configuration class for the Replay Engine.
 * Sets up all the beans needed for the application.
 */
@Configuration
@EnableConfigurationProperties({ReplayProperties.class, KafkaProperties.class})
@Import(AwsConfig.class)
public class ReplayEngineConfig {
    
    @Bean
    public KafkaProducer<String, String> kafkaProducer(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        
        // Basic producer configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // Performance and reliability configuration
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducer().getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getProducer().getRetries());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProperties.getProducer().getBatchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProperties.getProducer().getLingerMs());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaProperties.getProducer().getBufferMemory());
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, kafkaProperties.getProducer().getMaxRequestSize());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProperties.getProducer().getCompressionType());
        
        // Enable idempotence for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new KafkaProducer<>(props);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}