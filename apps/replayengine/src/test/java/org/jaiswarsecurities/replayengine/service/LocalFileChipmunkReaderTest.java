package org.jaiswarsecurities.replayengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jaiswarsecurities.replayengine.config.ReplayProperties;
import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalFileChipmunkReader.
 */
class LocalFileChipmunkReaderTest {
    
    @TempDir
    Path tempDir;
    
    private LocalFileChipmunkReader reader;
    private ReplayProperties replayProperties;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        replayProperties = new ReplayProperties();
        replayProperties.setSource(new ReplayProperties.Source());
        replayProperties.getSource().setType(ReplayProperties.Source.SourceType.LOCAL_FILE);
        replayProperties.getSource().setLocal(new ReplayProperties.Source.Local());
        
        objectMapper = new ObjectMapper();
        reader = new LocalFileChipmunkReader(replayProperties, objectMapper);
    }
    
    @Test
    void testReadEventsFromValidFile() throws IOException {
        // Create test file
        Path testFile = tempDir.resolve("test-events.json");
        String content = """
            {"timestamp": "2024-01-01T10:00:00Z", "trade_id": "T001", "symbol": "AAPL", "price": 150.0, "region": "US"}
            {"timestamp": "2024-01-01T10:00:01Z", "symbol": "AAPL", "price": 150.1, "bid": 149.9, "ask": 150.2, "region": "US"}
            {"timestamp": "2024-01-01T10:00:02Z", "base_currency": "USD", "target_currency": "EUR", "rate": 0.85, "region": "US"}
            """;
        Files.writeString(testFile, content);
        
        replayProperties.getSource().getLocal().setFilePath(testFile.toString());
        
        // Read events
        try (Stream<ChipmunkEvent> events = reader.readEvents()) {
            List<ChipmunkEvent> eventList = events.toList();
            
            assertEquals(3, eventList.size());
            
            // Check first event (trade)
            ChipmunkEvent tradeEvent = eventList.get(0);
            assertEquals(ChipmunkEvent.EventType.TRADE, tradeEvent.getEventType());
            assertEquals("US", tradeEvent.getRegion());
            assertNotNull(tradeEvent.getTimestamp());
            assertEquals(1L, tradeEvent.getLineNumber());
            
            // Check second event (market data)
            ChipmunkEvent marketEvent = eventList.get(1);
            assertEquals(ChipmunkEvent.EventType.MARKET_DATA, marketEvent.getEventType());
            
            // Check third event (FX rate)
            ChipmunkEvent fxEvent = eventList.get(2);
            assertEquals(ChipmunkEvent.EventType.FX_RATE, fxEvent.getEventType());
        }
    }
    
    @Test
    void testGetTotalEventCount() throws IOException {
        // Create test file
        Path testFile = tempDir.resolve("test-events.json");
        String content = """
            {"timestamp": "2024-01-01T10:00:00Z", "trade_id": "T001", "symbol": "AAPL", "price": 150.0}
            {"timestamp": "2024-01-01T10:00:01Z", "symbol": "AAPL", "price": 150.1}
            # This is a comment and should be ignored
            
            {"timestamp": "2024-01-01T10:00:02Z", "base_currency": "USD", "target_currency": "EUR", "rate": 0.85}
            """;
        Files.writeString(testFile, content);
        
        replayProperties.getSource().getLocal().setFilePath(testFile.toString());
        
        long count = reader.getTotalEventCount();
        assertEquals(3, count); // Only non-empty, non-comment lines should be counted
    }
    
    @Test
    void testReadEventsFromNonExistentFile() {
        replayProperties.getSource().getLocal().setFilePath("/nonexistent/file.json");
        
        assertThrows(IOException.class, () -> reader.readEvents());
    }
    
    @Test
    void testReadEventsSkipsInvalidLines() throws IOException {
        // Create test file with some invalid JSON lines
        Path testFile = tempDir.resolve("test-events.json");
        String content = """
            {"timestamp": "2024-01-01T10:00:00Z", "trade_id": "T001", "symbol": "AAPL", "price": 150.0}
            invalid json line
            {"timestamp": "2024-01-01T10:00:01Z", "symbol": "AAPL", "price": 150.1}
            { incomplete json
            {"timestamp": "2024-01-01T10:00:02Z", "base_currency": "USD", "target_currency": "EUR", "rate": 0.85}
            """;
        Files.writeString(testFile, content);
        
        replayProperties.getSource().getLocal().setFilePath(testFile.toString());
        
        try (Stream<ChipmunkEvent> events = reader.readEvents()) {
            List<ChipmunkEvent> eventList = events.toList();
            
            // Should only get valid events, invalid lines should be skipped
            assertEquals(3, eventList.size());
        }
    }
}