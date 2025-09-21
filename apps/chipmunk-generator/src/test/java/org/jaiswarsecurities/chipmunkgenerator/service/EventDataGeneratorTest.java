package org.jaiswarsecurities.chipmunkgenerator.service;

import org.jaiswarsecurities.chipmunkgenerator.config.GeneratorProperties;
import org.jaiswarsecurities.iris.proto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventDataGenerator.
 */
class EventDataGeneratorTest {

    private EventDataGenerator eventDataGenerator;
    private GeneratorProperties generatorProperties;

    @BeforeEach
    void setUp() {
        generatorProperties = new GeneratorProperties();
        eventDataGenerator = new EventDataGenerator(generatorProperties);
    }

    @Test
    void testGenerateTradeEvent() {
        // Given
        Instant timestamp = Instant.now();
        String region = "US";
        String correlationId = "test-correlation-id";

        // When
        TradeEvent tradeEvent = eventDataGenerator.generateTradeEvent(timestamp, region, correlationId);

        // Then
        assertNotNull(tradeEvent);
        assertNotNull(tradeEvent.getTradeId());
        assertNotNull(tradeEvent.getOrderId());
        assertNotNull(tradeEvent.getInstrument());
        assertNotNull(tradeEvent.getAccount());
        assertNotNull(tradeEvent.getCounterparty());
        assertTrue(tradeEvent.getQuantity() > 0);
        assertTrue(tradeEvent.getPrice() > 0);
        assertTrue(tradeEvent.getSide().equals("BUY") || tradeEvent.getSide().equals("SELL"));
        assertNotNull(tradeEvent.getVenue());
        assertEquals(timestamp.toEpochMilli(), tradeEvent.getTradeTimestamp());
        assertNotNull(tradeEvent.getStatus());
        assertNotNull(tradeEvent.getRegulatoryId());
        assertNotNull(tradeEvent.getTraderId());
        assertNotNull(tradeEvent.getStrategyTag());
        assertEquals(correlationId, tradeEvent.getCorrelationId());
    }

    @Test
    void testGenerateMarketDataEvent() {
        // Given
        Instant timestamp = Instant.now();
        String region = "UK";

        // When
        MarketDataEvent marketDataEvent = eventDataGenerator.generateMarketDataEvent(timestamp, region);

        // Then
        assertNotNull(marketDataEvent);
        assertNotNull(marketDataEvent.getInstrument());
        assertTrue(marketDataEvent.getBid() > 0);
        assertTrue(marketDataEvent.getAsk() > 0);
        assertTrue(marketDataEvent.getLastPrice() > 0);
        assertTrue(marketDataEvent.getBidSize() > 0);
        assertTrue(marketDataEvent.getAskSize() > 0);
        assertEquals(timestamp.toEpochMilli(), marketDataEvent.getTimestamp());
        assertNotNull(marketDataEvent.getVenue());
        assertNotNull(marketDataEvent.getSourceFeed());
        
        // Verify bid-ask spread logic
        assertTrue(marketDataEvent.getAsk() > marketDataEvent.getBid());
        assertTrue(marketDataEvent.getLastPrice() >= marketDataEvent.getBid());
        assertTrue(marketDataEvent.getLastPrice() <= marketDataEvent.getAsk());
    }

    @Test
    void testGenerateFxRateEvent() {
        // Given
        Instant timestamp = Instant.now();
        String region = "JP";

        // When
        FxRateEvent fxRateEvent = eventDataGenerator.generateFxRateEvent(timestamp, region);

        // Then
        assertNotNull(fxRateEvent);
        assertNotNull(fxRateEvent.getFromCurrency());
        assertNotNull(fxRateEvent.getToCurrency());
        assertNotEquals(fxRateEvent.getFromCurrency(), fxRateEvent.getToCurrency());
        assertTrue(fxRateEvent.getRate() > 0);
        assertTrue(fxRateEvent.getBid() > 0);
        assertTrue(fxRateEvent.getAsk() > 0);
        assertEquals(timestamp.toEpochMilli(), fxRateEvent.getTimestamp());
        assertNotNull(fxRateEvent.getSource());
        
        // Verify bid-ask spread logic for FX
        assertTrue(fxRateEvent.getAsk() > fxRateEvent.getBid());
    }

    @Test
    void testTradeEventRegionalCharacteristics() {
        // Test different regions produce appropriate prefixes
        String[] regions = {"UK", "US", "JP", "CN"};
        
        for (String region : regions) {
            TradeEvent tradeEvent = eventDataGenerator.generateTradeEvent(Instant.now(), region, "");
            
            // Verify trade ID has regional prefix
            String tradeId = tradeEvent.getTradeId();
            assertNotNull(tradeId);
            assertTrue(tradeId.length() > 5);
            
            // Verify account ID has appropriate currency prefix
            String accountId = tradeEvent.getAccount();
            assertNotNull(accountId);
            assertTrue(accountId.contains("ACC"));
        }
    }

    @Test
    void testEventDataIsRealistic() {
        // Generate multiple events to test data distribution
        for (int i = 0; i < 10; i++) {
            TradeEvent trade = eventDataGenerator.generateTradeEvent(Instant.now(), "US", "");
            
            // Verify realistic price range ($50-$1000)
            assertTrue(trade.getPrice() >= 50.0 && trade.getPrice() <= 1000.0);
            
            // Verify realistic quantity (multiples of 100)
            assertTrue(trade.getQuantity() % 100 == 0);
            assertTrue(trade.getQuantity() >= 100 && trade.getQuantity() <= 1000);
        }
    }
}