package org.jaiswarsecurities.chipmunkgenerator.service;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.chipmunkgenerator.config.GeneratorProperties;
import org.jaiswarsecurities.iris.proto.*;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service for generating Chipmunk files with realistic financial event data.
 * Creates files in Protobuf format and uploads them to MinIO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChipmunkFileGenerator {

    private final GeneratorProperties generatorProperties;
    private final S3Client s3Client;
    private final EventDataGenerator eventDataGenerator;
    private final Random random = new Random();

    /**
     * Generates a chipmunk file with the specified number of events and uploads it to MinIO.
     *
     * @param fileName   Name of the file to generate
     * @param eventCount Number of events to generate
     * @return The S3 path where the file was uploaded
     * @throws IOException If file generation or upload fails
     */
    public String generateAndUploadChipmunkFile(String fileName, int eventCount) throws IOException {
        log.info("Starting generation of {} events for file: {}", eventCount, fileName);

        // Generate events
        List<ChipmunkRecord> records = generateChipmunkRecords(eventCount);
        
        // Serialize to binary format
        byte[] fileContent = serializeRecords(records);
        
        // Upload to MinIO
        String s3Key = generatorProperties.getBasePath() + "/" + fileName;
        uploadToMinIO(s3Key, fileContent);
        
        log.info("Successfully generated and uploaded {} records ({} bytes) to s3://{}/{}",
                records.size(), fileContent.length, generatorProperties.getBucketName(), s3Key);
        
        return String.format("s3://%s/%s", generatorProperties.getBucketName(), s3Key);
    }

    /**
     * Generates a list of ChipmunkRecord objects with mixed event types.
     */
    private List<ChipmunkRecord> generateChipmunkRecords(int eventCount) {
        List<ChipmunkRecord> records = new ArrayList<>();
        Instant baseTime = Instant.now().minus(generatorProperties.getEventGeneration().getTimeRangeHours(), ChronoUnit.HOURS);
        
        for (int i = 0; i < eventCount; i++) {
            // Determine event type based on configured ratios
            String eventType = determineEventType();
            
            // Generate timestamp with realistic distribution
            Instant eventTime = baseTime.plus((long) (random.nextDouble() * generatorProperties.getEventGeneration().getTimeRangeHours() * 3600), ChronoUnit.SECONDS);
            
            // Select random region
            String region = generatorProperties.getRegions()[random.nextInt(generatorProperties.getRegions().length)];
            
            // Generate correlation ID for some events
            String correlationId = random.nextDouble() < 0.3 ? UUID.randomUUID().toString() : "";
            
            ChipmunkRecord record = createChipmunkRecord(eventType, eventTime, region, correlationId);
            records.add(record);
        }
        
        // Sort by timestamp to maintain chronological order
        records.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        
        return records;
    }

    /**
     * Creates a ChipmunkRecord based on the event type.
     */
    private ChipmunkRecord createChipmunkRecord(String eventType, Instant timestamp, String region, String correlationId) {
        ChipmunkRecord.Builder recordBuilder = ChipmunkRecord.newBuilder()
                .setEventType(eventType)
                .setTimestamp(timestamp.toEpochMilli())
                .setRegion(region)
                .setCorrelationId(correlationId);

        switch (eventType) {
            case "TRADE":
                TradeEvent tradeEvent = eventDataGenerator.generateTradeEvent(timestamp, region, correlationId);
                recordBuilder.setPayload(ByteString.copyFrom(tradeEvent.toByteArray()));
                break;
            case "MARKET_DATA":
                MarketDataEvent marketDataEvent = eventDataGenerator.generateMarketDataEvent(timestamp, region);
                recordBuilder.setPayload(ByteString.copyFrom(marketDataEvent.toByteArray()));
                break;
            case "FX":
                FxRateEvent fxRateEvent = eventDataGenerator.generateFxRateEvent(timestamp, region);
                recordBuilder.setPayload(ByteString.copyFrom(fxRateEvent.toByteArray()));
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        return recordBuilder.build();
    }

    /**
     * Determines the event type based on configured ratios.
     */
    private String determineEventType() {
        double rand = random.nextDouble();
        GeneratorProperties.EventGeneration config = generatorProperties.getEventGeneration();
        
        if (rand < config.getTradeEventRatio()) {
            return "TRADE";
        } else if (rand < config.getTradeEventRatio() + config.getMarketDataEventRatio()) {
            return "MARKET_DATA";
        } else {
            return "FX";
        }
    }

    /**
     * Serializes ChipmunkRecord objects to binary format.
     * Each record is length-prefixed for parsing.
     */
    private byte[] serializeRecords(List<ChipmunkRecord> records) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for (ChipmunkRecord record : records) {
            byte[] recordBytes = record.toByteArray();
            
            // Write length prefix (4 bytes, big-endian)
            baos.write((recordBytes.length >>> 24) & 0xFF);
            baos.write((recordBytes.length >>> 16) & 0xFF);
            baos.write((recordBytes.length >>> 8) & 0xFF);
            baos.write(recordBytes.length & 0xFF);
            
            // Write record data
            baos.write(recordBytes);
        }
        
        return baos.toByteArray();
    }

    /**
     * Uploads the generated file content to MinIO.
     */
    private void uploadToMinIO(String key, byte[] content) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(generatorProperties.getBucketName())
                    .key(key)
                    .contentType("application/octet-stream")
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            
            log.info("Successfully uploaded file to MinIO: s3://{}/{}", 
                    generatorProperties.getBucketName(), key);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: s3://{}/{}", 
                    generatorProperties.getBucketName(), key, e);
            throw new RuntimeException("Failed to upload to MinIO", e);
        }
    }
}