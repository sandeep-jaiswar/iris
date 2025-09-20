package org.jaiswarsecurities.replayengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.replayengine.config.ReplayProperties;
import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Implementation of ChipmunkReader that reads files from MinIO/S3.
 * Uses the existing S3Client from awsconfig library.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinIOChipmunkReader implements ChipmunkReader {
    
    private final S3Client s3Client;
    private final ReplayProperties replayProperties;
    private final ObjectMapper objectMapper;
    
    @Override
    public Stream<ChipmunkEvent> readEvents() throws IOException {
        ReplayProperties.Source.Minio minioConfig = replayProperties.getSource().getMinio();
        
        log.info("Reading Chipmunk file from MinIO: bucket={}, key={}", 
                minioConfig.getBucketName(), minioConfig.getObjectKey());
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(minioConfig.getBucketName())
                    .key(minioConfig.getObjectKey())
                    .build();
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            s3Client.getObject(getObjectRequest), 
                            StandardCharsets.UTF_8
                    )
            );
            
            AtomicLong lineNumber = new AtomicLong(0);
            
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                    .map(line -> parseChipmunkLine(line, lineNumber.incrementAndGet()))
                    .filter(event -> event != null);
                    
        } catch (Exception e) {
            log.error("Error reading from MinIO: bucket={}, key={}", 
                    minioConfig.getBucketName(), minioConfig.getObjectKey(), e);
            throw new IOException("Failed to read Chipmunk file from MinIO", e);
        }
    }
    
    @Override
    public long getTotalEventCount() throws IOException {
        // For streaming from MinIO, we can't efficiently get line count without reading the entire file
        // In a production system, you might want to store metadata about files
        log.debug("Total event count not available for streaming MinIO reads");
        return -1;
    }
    
    @Override
    public void close() throws IOException {
        // S3Client is managed by Spring, no explicit cleanup needed
        log.debug("MinIOChipmunkReader closed");
    }
    
    /**
     * Parses a line from the Chipmunk file into a ChipmunkEvent.
     * Expected format: JSON objects, one per line.
     */
    private ChipmunkEvent parseChipmunkLine(String line, long lineNumber) {
        try {
            // Parse the JSON line into a map
            Map<String, Object> data = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
            
            // Determine event type based on the data content
            ChipmunkEvent.EventType eventType = determineEventType(data);
            
            // Extract timestamp (assume ISO format or epoch millis)
            Instant timestamp = extractTimestamp(data);
            
            // Extract region
            String region = extractRegion(data);
            
            return ChipmunkEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .timestamp(timestamp)
                    .region(region)
                    .data(data)
                    .jsonPayload(line)
                    .lineNumber(lineNumber)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Failed to parse line {}: {}", lineNumber, line, e);
            return null;  // Skip invalid lines
        }
    }
    
    private ChipmunkEvent.EventType determineEventType(Map<String, Object> data) {
        // Simple heuristic based on field names - in real implementation,
        // this would be based on the actual Chipmunk file format specification
        if (data.containsKey("trade_id") || data.containsKey("tradeId")) {
            return ChipmunkEvent.EventType.TRADE;
        } else if (data.containsKey("symbol") && data.containsKey("price")) {
            return ChipmunkEvent.EventType.MARKET_DATA;
        } else if (data.containsKey("currency_pair") || data.containsKey("base_currency")) {
            return ChipmunkEvent.EventType.FX_RATE;
        } else {
            // Default to trade if we can't determine
            return ChipmunkEvent.EventType.TRADE;
        }
    }
    
    private Instant extractTimestamp(Map<String, Object> data) {
        // Try to extract timestamp from common field names
        Object timestamp = data.get("timestamp");
        if (timestamp == null) {
            timestamp = data.get("time");
        }
        if (timestamp == null) {
            timestamp = data.get("event_time");
        }
        
        if (timestamp instanceof String) {
            try {
                return Instant.parse((String) timestamp);
            } catch (Exception e) {
                // If parsing fails, try as epoch millis
                try {
                    return Instant.ofEpochMilli(Long.parseLong((String) timestamp));
                } catch (Exception e2) {
                    log.debug("Could not parse timestamp: {}", timestamp);
                    return Instant.now();
                }
            }
        } else if (timestamp instanceof Number) {
            return Instant.ofEpochMilli(((Number) timestamp).longValue());
        } else {
            return Instant.now();
        }
    }
    
    private String extractRegion(Map<String, Object> data) {
        Object region = data.get("region");
        if (region instanceof String) {
            return (String) region;
        }
        
        // Try to infer from other fields or use a default
        return "UNKNOWN";
    }
}