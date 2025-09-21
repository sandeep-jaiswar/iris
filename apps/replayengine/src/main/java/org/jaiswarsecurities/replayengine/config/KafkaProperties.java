package org.jaiswarsecurities.replayengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for Kafka.
 * Binds to the 'kafka' section in replay-config.yaml.
 */
@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    
    private String bootstrapServers = "localhost:9092";
    
    @NestedConfigurationProperty
    private Producer producer = new Producer();
    
    @NestedConfigurationProperty
    private Topics topics = new Topics();
    
    @Data
    public static class Producer {
        private String acks = "1";
        private int retries = 3;
        private int batchSize = 16384;
        private int lingerMs = 5;
        private long bufferMemory = 33554432L;
        private int maxRequestSize = 1048576;
        private String compressionType = "lz4";
    }
    
    @Data
    public static class Topics {
        private String tradeEvents = "trade-events";
        private String marketData = "market-data";
        private String fxRates = "fx-rates";
    }
}