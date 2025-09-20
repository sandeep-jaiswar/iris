package org.jaiswarsecurities.replayengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a parsed event from a Chipmunk file.
 * This is the canonical event format used throughout the IRIS system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChipmunkEvent {
    
    /**
     * Unique identifier for this event
     */
    private String eventId;
    
    /**
     * Type of the event: TRADE, MARKET_DATA, FX_RATE
     */
    private EventType eventType;
    
    /**
     * Original timestamp when the event occurred
     */
    private Instant timestamp;
    
    /**
     * Region where the event originated: UK, US, JAPAN, CHINA
     */
    private String region;
    
    /**
     * The raw event data as key-value pairs
     */
    private Map<String, Object> data;
    
    /**
     * The serialized JSON representation of the event
     */
    private String jsonPayload;
    
    /**
     * The original line number in the chipmunk file (for debugging/replay tracking)
     */
    private long lineNumber;
    
    public enum EventType {
        TRADE("trade-events"),
        MARKET_DATA("market-data"),
        FX_RATE("fx-rates");
        
        private final String topicName;
        
        EventType(String topicName) {
            this.topicName = topicName;
        }
        
        public String getTopicName() {
            return topicName;
        }
    }
}