package org.jaiswarsecurities.replayengine.service;

import org.jaiswarsecurities.replayengine.config.ReplayProperties;
import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating the complete replay engine functionality.
 * Tests the full pipeline from file reading to event parsing.
 */
class ReplayEngineIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private ChipmunkReader chipmunkReader;
    private ReplayProperties replayProperties;
    
    @BeforeEach
    void setUp() {
        // Setup configuration
        replayProperties = new ReplayProperties();
        replayProperties.setSpeedMode(ReplayProperties.SpeedMode.BURST);
        replayProperties.setBurstBatchSize(10);
        
        ReplayProperties.Source source = new ReplayProperties.Source();
        source.setType(ReplayProperties.Source.SourceType.LOCAL_FILE);
        source.setLocal(new ReplayProperties.Source.Local());
        replayProperties.setSource(source);
        
        // Create reader
        ObjectMapper objectMapper = new ObjectMapper();
        chipmunkReader = new LocalFileChipmunkReader(replayProperties, objectMapper);
    }
    
    @Test
    void testCompleteReplayFlow() throws IOException {
        // Create a realistic test file with mixed event types
        Path testFile = tempDir.resolve("sample-trading-data.chipmunk");
        String content = """
            {"timestamp": "2024-01-01T10:00:00.000Z", "trade_id": "T001", "symbol": "AAPL", "price": 150.0, "quantity": 100, "region": "US"}
            {"timestamp": "2024-01-01T10:00:01.000Z", "symbol": "AAPL", "price": 150.1, "bid": 149.9, "ask": 150.2, "region": "US"}
            {"timestamp": "2024-01-01T10:00:02.000Z", "base_currency": "USD", "target_currency": "EUR", "rate": 0.85, "region": "US"}
            {"timestamp": "2024-01-01T10:00:03.000Z", "trade_id": "T002", "symbol": "GOOGL", "price": 2800.0, "quantity": 50, "region": "US"}
            {"timestamp": "2024-01-01T10:00:04.000Z", "symbol": "GOOGL", "price": 2801.0, "bid": 2799.5, "ask": 2802.0, "region": "US"}
            {"timestamp": "2024-01-01T10:00:05.000Z", "base_currency": "GBP", "target_currency": "USD", "rate": 1.25, "region": "UK"}
            {"timestamp": "2024-01-01T10:00:06.000Z", "trade_id": "T003", "symbol": "TSLA", "price": 900.0, "quantity": 75, "region": "US"}
            {"timestamp": "2024-01-01T10:00:07.000Z", "symbol": "TSLA", "price": 901.5, "bid": 900.0, "ask": 903.0, "region": "US"}
            {"timestamp": "2024-01-01T10:00:08.000Z", "base_currency": "JPY", "target_currency": "USD", "rate": 0.0067, "region": "JAPAN"}
            {"timestamp": "2024-01-01T10:00:09.000Z", "trade_id": "T004", "symbol": "MSFT", "price": 420.0, "quantity": 200, "region": "US"}
            """;
        Files.writeString(testFile, content);
        
        replayProperties.getSource().getLocal().setFilePath(testFile.toString());
        
        // Test reading and event classification
        try (Stream<ChipmunkEvent> events = chipmunkReader.readEvents()) {
            List<ChipmunkEvent> eventList = events.toList();
            
            // Verify total count
            assertEquals(10, eventList.size());
            
            // Count events by type
            long tradeEvents = eventList.stream()
                    .filter(e -> e.getEventType() == ChipmunkEvent.EventType.TRADE)
                    .count();
            long marketDataEvents = eventList.stream()
                    .filter(e -> e.getEventType() == ChipmunkEvent.EventType.MARKET_DATA)
                    .count();
            long fxRateEvents = eventList.stream()
                    .filter(e -> e.getEventType() == ChipmunkEvent.EventType.FX_RATE)
                    .count();
            
            // Verify event type distribution
            assertEquals(4, tradeEvents); // T001, T002, T003, T004
            assertEquals(3, marketDataEvents); // AAPL, GOOGL, TSLA market data
            assertEquals(3, fxRateEvents); // USD/EUR, GBP/USD, JPY/USD
            
            // Test specific event properties
            ChipmunkEvent firstTrade = eventList.stream()
                    .filter(e -> e.getEventType() == ChipmunkEvent.EventType.TRADE)
                    .findFirst()
                    .orElseThrow();
                    
            assertNotNull(firstTrade.getEventId());
            assertNotNull(firstTrade.getTimestamp());
            assertEquals("US", firstTrade.getRegion());
            assertTrue(firstTrade.getData().containsKey("trade_id"));
            assertEquals(1L, firstTrade.getLineNumber());
            
            // Verify all events have required fields
            for (ChipmunkEvent event : eventList) {
                assertNotNull(event.getEventId(), "Event ID should not be null");
                assertNotNull(event.getEventType(), "Event type should not be null");
                assertNotNull(event.getTimestamp(), "Timestamp should not be null");
                assertNotNull(event.getRegion(), "Region should not be null");
                assertNotNull(event.getData(), "Data should not be null");
                assertNotNull(event.getJsonPayload(), "JSON payload should not be null");
                assertTrue(event.getLineNumber() > 0, "Line number should be positive");
            }
            
            // Verify timestamp ordering (events should maintain chronological order)
            for (int i = 1; i < eventList.size(); i++) {
                ChipmunkEvent prev = eventList.get(i - 1);
                ChipmunkEvent current = eventList.get(i);
                assertTrue(
                    current.getTimestamp().compareTo(prev.getTimestamp()) >= 0,
                    "Events should be in chronological order"
                );
            }
        }
        
        // Test total count method
        long totalCount = chipmunkReader.getTotalEventCount();
        assertEquals(10, totalCount);
    }
    
    @Test
    void testEventTypeClassification() throws IOException {
        // Test specific event type classification logic
        Path testFile = tempDir.resolve("classification-test.chipmunk");
        String content = """
            {"trade_id": "T001", "symbol": "AAPL", "price": 150.0, "timestamp": "2024-01-01T10:00:00Z"}
            {"symbol": "AAPL", "price": 150.1, "bid": 149.9, "ask": 150.2, "timestamp": "2024-01-01T10:00:01Z"}
            {"base_currency": "USD", "target_currency": "EUR", "rate": 0.85, "timestamp": "2024-01-01T10:00:02Z"}
            {"currency_pair": "GBP/USD", "rate": 1.25, "timestamp": "2024-01-01T10:00:03Z"}
            {"tradeId": "T002", "symbol": "GOOGL", "price": 2800.0, "timestamp": "2024-01-01T10:00:04Z"}
            """;
        Files.writeString(testFile, content);
        
        replayProperties.getSource().getLocal().setFilePath(testFile.toString());
        
        try (Stream<ChipmunkEvent> events = chipmunkReader.readEvents()) {
            List<ChipmunkEvent> eventList = events.toList();
            
            assertEquals(ChipmunkEvent.EventType.TRADE, eventList.get(0).getEventType());
            assertEquals(ChipmunkEvent.EventType.MARKET_DATA, eventList.get(1).getEventType());
            assertEquals(ChipmunkEvent.EventType.FX_RATE, eventList.get(2).getEventType());
            assertEquals(ChipmunkEvent.EventType.FX_RATE, eventList.get(3).getEventType());
            assertEquals(ChipmunkEvent.EventType.TRADE, eventList.get(4).getEventType());
        }
    }
}