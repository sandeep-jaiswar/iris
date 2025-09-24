package org.jaiswarsecurities.chipmunkgenerator.service;

import org.jaiswarsecurities.chipmunkgenerator.config.GeneratorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for ChipmunkFileGenerator.
 */
@ExtendWith(MockitoExtension.class)
class ChipmunkFileGeneratorTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private EventDataGenerator eventDataGenerator;

    private ChipmunkFileGenerator chipmunkFileGenerator;
    private GeneratorProperties generatorProperties;

    @BeforeEach
    void setUp() {
        generatorProperties = new GeneratorProperties();
        chipmunkFileGenerator = new ChipmunkFileGenerator(
            generatorProperties, s3Client, eventDataGenerator);
    }

    @Test
    void testGenerateAndUploadChipmunkFile() throws Exception {
        // Given
        String fileName = "test-events.chip";
        int eventCount = 5; // Small count for testing
        
        // Mock event generation responses - use lenient() to avoid unnecessary stubbing errors
        lenient().when(eventDataGenerator.generateTradeEvent(any(), any(), any()))
            .thenReturn(org.jaiswarsecurities.iris.proto.TradeEvent.newBuilder()
                .setTradeId("TEST-001")
                .setInstrument("AAPL")
                .setPrice(150.0)
                .setQuantity(100)
                .setSide("BUY")
                .setVenue("NYSE")
                .setTradeTimestamp(System.currentTimeMillis())
                .build());
                
        lenient().when(eventDataGenerator.generateMarketDataEvent(any(), any()))
            .thenReturn(org.jaiswarsecurities.iris.proto.MarketDataEvent.newBuilder()
                .setInstrument("AAPL")
                .setBid(149.5)
                .setAsk(150.5)
                .setLastPrice(150.0)
                .setTimestamp(System.currentTimeMillis())
                .build());
                
        lenient().when(eventDataGenerator.generateFxRateEvent(any(), any()))
            .thenReturn(org.jaiswarsecurities.iris.proto.FxRateEvent.newBuilder()
                .setFromCurrency("USD")
                .setToCurrency("EUR")
                .setRate(0.85)
                .setBid(0.849)
                .setAsk(0.851)
                .setTimestamp(System.currentTimeMillis())
                .build());
        
        // Mock S3 upload response
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = chipmunkFileGenerator.generateAndUploadChipmunkFile(fileName, eventCount);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(generatorProperties.getBucketName()));
        assertTrue(result.contains(fileName));
        assertTrue(result.startsWith("s3://"));
        
        // Verify S3 upload was called
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testConfigurationDefaults() {
        // Verify default configuration values
        assertEquals("chipmunk-archive", generatorProperties.getBucketName());
        assertEquals("generated", generatorProperties.getBasePath());
        assertArrayEquals(new String[]{"UK", "US", "JP", "CN"}, generatorProperties.getRegions());
        
        GeneratorProperties.EventGeneration eventGen = generatorProperties.getEventGeneration();
        assertEquals(0.5, eventGen.getTradeEventRatio(), 0.001);
        assertEquals(0.3, eventGen.getMarketDataEventRatio(), 0.001);
        assertEquals(0.2, eventGen.getFxRateEventRatio(), 0.001);
        assertEquals(24, eventGen.getTimeRangeHours());
        
        assertTrue(eventGen.getInstruments().length > 0);
        assertTrue(eventGen.getCurrencyPairs().length > 0);
        assertTrue(eventGen.getVenues().length > 0);
    }

    @Test
    void testEventRatiosSum() {
        // Verify that event ratios sum to approximately 1.0
        GeneratorProperties.EventGeneration eventGen = generatorProperties.getEventGeneration();
        double sum = eventGen.getTradeEventRatio() + 
                    eventGen.getMarketDataEventRatio() + 
                    eventGen.getFxRateEventRatio();
        
        assertEquals(1.0, sum, 0.001);
    }
}