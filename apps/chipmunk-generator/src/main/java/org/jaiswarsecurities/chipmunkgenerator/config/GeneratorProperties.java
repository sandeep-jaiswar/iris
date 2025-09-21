package org.jaiswarsecurities.chipmunkgenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Chipmunk Generator.
 */
@Data
@Component
@ConfigurationProperties(prefix = "chipmunk.generator")
public class GeneratorProperties {
    
    /**
     * MinIO bucket name for uploading generated files
     */
    private String bucketName = "chipmunk-archive";
    
    /**
     * Base path within the bucket for generated files
     */
    private String basePath = "generated";
    
    /**
     * Regions to generate events for
     */
    private String[] regions = {"UK", "US", "JP", "CN"};
    
    /**
     * Event generation configuration
     */
    private EventGeneration eventGeneration = new EventGeneration();
    
    @Data
    public static class EventGeneration {
        /**
         * Percentage of trade events (0.0 to 1.0)
         */
        private double tradeEventRatio = 0.5;
        
        /**
         * Percentage of market data events (0.0 to 1.0)
         */
        private double marketDataEventRatio = 0.3;
        
        /**
         * Percentage of FX rate events (0.0 to 1.0)
         */
        private double fxRateEventRatio = 0.2;
        
        /**
         * Time range for events in hours
         */
        private int timeRangeHours = 24;
        
        /**
         * Base instruments for market data and trades
         */
        private String[] instruments = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX",
            "ORCL", "CRM", "INTC", "AMD", "ADBE", "PYPL", "UBER", "SPOT"
        };
        
        /**
         * Currency pairs for FX events
         */
        private String[] currencyPairs = {
            "USD/EUR", "USD/GBP", "USD/JPY", "USD/CNY", "EUR/GBP", 
            "EUR/JPY", "GBP/JPY", "AUD/USD", "USD/CAD", "USD/CHF"
        };
        
        /**
         * Trading venues
         */
        private String[] venues = {
            "LSE", "NYSE", "NASDAQ", "TSE", "SSE", "XTRA", "EURONEXT"
        };
    }
}