package org.jaiswarsecurities.replayengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the Replay Engine.
 * Binds to the 'replay' section in replay-config.yaml.
 */
@Data
@ConfigurationProperties(prefix = "replay")
public class ReplayProperties {
    
    /**
     * Replay speed mode: real-time, accelerated, burst
     */
    private SpeedMode speedMode = SpeedMode.REAL_TIME;
    
    /**
     * Speed multiplier for accelerated mode (ignored in real-time and burst modes)
     */
    private double speedMultiplier = 1.0;
    
    /**
     * Batch size for burst mode
     */
    private int burstBatchSize = 1000;
    
    @NestedConfigurationProperty
    private Source source = new Source();
    
    @NestedConfigurationProperty
    private Checkpoint checkpoint = new Checkpoint();
    
    public enum SpeedMode {
        REAL_TIME("real-time"),
        ACCELERATED("accelerated"),
        BURST("burst");
        
        private final String value;
        
        SpeedMode(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static SpeedMode fromValue(String value) {
            for (SpeedMode mode : values()) {
                if (mode.value.equals(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown speed mode: " + value);
        }
    }
    
    @Data
    public static class Source {
        /**
         * Source type: minio, local-file
         */
        private SourceType type = SourceType.MINIO;
        
        @NestedConfigurationProperty
        private Minio minio = new Minio();
        
        @NestedConfigurationProperty
        private Local local = new Local();
        
        public enum SourceType {
            MINIO, LOCAL_FILE
        }
        
        @Data
        public static class Minio {
            private String bucketName = "iris-chipmunk-files";
            private String objectKey;
        }
        
        @Data
        public static class Local {
            private String filePath;
        }
    }
    
    @Data
    public static class Checkpoint {
        private boolean enabled = true;
        private int intervalSeconds = 30;
        private StorageType storageType = StorageType.FILE;
        private String filePath = "/tmp/replay-checkpoint.json";
        
        public enum StorageType {
            FILE, DYNAMODB
        }
    }
}