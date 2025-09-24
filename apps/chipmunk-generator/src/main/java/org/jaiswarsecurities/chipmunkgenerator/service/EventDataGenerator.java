package org.jaiswarsecurities.chipmunkgenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.chipmunkgenerator.config.GeneratorProperties;
import org.jaiswarsecurities.iris.proto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for generating realistic financial event data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventDataGenerator {

    private final GeneratorProperties generatorProperties;
    private final Random random = new Random();

    /**
     * Generates a realistic TradeEvent.
     */
    public TradeEvent generateTradeEvent(Instant timestamp, String region, String correlationId) {
        GeneratorProperties.EventGeneration config = generatorProperties.getEventGeneration();
        
        String instrument = config.getInstruments()[random.nextInt(config.getInstruments().length)];
        String venue = config.getVenues()[random.nextInt(config.getVenues().length)];
        
        // Generate realistic price and quantity
        double basePrice = 50.0 + random.nextDouble() * 950.0; // $50 to $1000
        double quantity = (random.nextInt(10) + 1) * 100; // 100 to 1000 shares
        
        String side = random.nextBoolean() ? "BUY" : "SELL";
        String[] statuses = {"FILLED", "PARTIALLY_FILLED", "NEW"};
        String status = statuses[random.nextInt(statuses.length)];
        
        String[] strategies = {"ALGO", "MANUAL", "HFT", "TWAP", "VWAP"};
        String strategy = strategies[random.nextInt(strategies.length)];

        return TradeEvent.newBuilder()
                .setTradeId(generateTradeId(region))
                .setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8))
                .setInstrument(instrument)
                .setAccount(generateAccountId(region))
                .setCounterparty(generateCounterparty(venue))
                .setQuantity(quantity)
                .setPrice(roundToTwoDecimalPlaces(basePrice))
                .setSide(side)
                .setVenue(venue)
                .setTradeTimestamp(timestamp.toEpochMilli())
                .setStatus(status)
                .setRegulatoryId(generateRegulatoryId(region))
                .setTraderId(generateTraderId())
                .setStrategyTag(strategy)
                .setCorrelationId(correlationId)
                .build();
    }

    /**
     * Generates a realistic MarketDataEvent.
     */
    public MarketDataEvent generateMarketDataEvent(Instant timestamp, String region) {
        GeneratorProperties.EventGeneration config = generatorProperties.getEventGeneration();
        
        String instrument = config.getInstruments()[random.nextInt(config.getInstruments().length)];
        String venue = config.getVenues()[random.nextInt(config.getVenues().length)];
        
        // Generate realistic market data with bid-ask spread
        double midPrice = 50.0 + random.nextDouble() * 950.0;
        double spread = midPrice * (0.0001 + random.nextDouble() * 0.002); // 1-20 bps spread
        
        double bid = midPrice - spread / 2;
        double ask = midPrice + spread / 2;
        double lastPrice = bid + random.nextDouble() * spread;
        
        // Generate realistic sizes
        double bidSize = (random.nextInt(50) + 1) * 100;
        double askSize = (random.nextInt(50) + 1) * 100;
        
        String[] feeds = {"Bloomberg", "Reuters", "DirectFeed", "IEX", "ARCA"};
        String sourceFeed = feeds[random.nextInt(feeds.length)];

        return MarketDataEvent.newBuilder()
                .setInstrument(instrument)
                .setBid(roundToTwoDecimalPlaces(bid))
                .setAsk(roundToTwoDecimalPlaces(ask))
                .setLastPrice(roundToTwoDecimalPlaces(lastPrice))
                .setBidSize(bidSize)
                .setAskSize(askSize)
                .setTimestamp(timestamp.toEpochMilli())
                .setVenue(venue)
                .setSourceFeed(sourceFeed)
                .build();
    }

    /**
     * Generates a realistic FxRateEvent.
     */
    public FxRateEvent generateFxRateEvent(Instant timestamp, String region) {
        GeneratorProperties.EventGeneration config = generatorProperties.getEventGeneration();
        
        String currencyPair = config.getCurrencyPairs()[random.nextInt(config.getCurrencyPairs().length)];
        String[] currencies = currencyPair.split("/");
        String fromCurrency = currencies[0];
        String toCurrency = currencies[1];
        
        // Generate realistic FX rates based on currency pair
        double baseRate = getBaseRate(fromCurrency, toCurrency);
        double spread = baseRate * (0.0001 + random.nextDouble() * 0.0005); // 1-5 pips spread
        
        double midRate = baseRate * (0.95 + random.nextDouble() * 0.1); // ±5% variation
        double bid = midRate - spread / 2;
        double ask = midRate + spread / 2;
        
        String[] sources = {"EBS", "Refinitiv", "Bloomberg", "FXAll", "Currenex"};
        String source = sources[random.nextInt(sources.length)];

        return FxRateEvent.newBuilder()
                .setFromCurrency(fromCurrency)
                .setToCurrency(toCurrency)
                .setRate(roundToFiveDecimalPlaces(midRate))
                .setBid(roundToFiveDecimalPlaces(bid))
                .setAsk(roundToFiveDecimalPlaces(ask))
                .setTimestamp(timestamp.toEpochMilli())
                .setSource(source)
                .build();
    }

    /**
     * Gets a base exchange rate for currency pairs.
     */
    private double getBaseRate(String fromCurrency, String toCurrency) {
        String pair = fromCurrency + "/" + toCurrency;
        return switch (pair) {
            case "USD/EUR" -> 0.85;
            case "USD/GBP" -> 0.75;
            case "USD/JPY" -> 110.0;
            case "USD/CNY" -> 7.0;
            case "EUR/GBP" -> 0.88;
            case "EUR/JPY" -> 130.0;
            case "GBP/JPY" -> 147.0;
            case "AUD/USD" -> 0.72;
            case "USD/CAD" -> 1.25;
            case "USD/CHF" -> 0.92;
            default -> 1.0;
        };
    }

    /**
     * Generates a trade ID with regional prefix.
     */
    private String generateTradeId(String region) {
        String prefix = switch (region) {
            case "UK" -> "LON";
            case "US" -> "NYC";
            case "JP" -> "TKY";
            case "CN" -> "SHG";
            default -> "GLB";
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    /**
     * Generates an account ID with regional characteristics.
     */
    private String generateAccountId(String region) {
        String prefix = switch (region) {
            case "UK" -> "GBP";
            case "US" -> "USD";
            case "JP" -> "JPY";
            case "CN" -> "CNY";
            default -> "GLB";
        };
        return prefix + "-ACC-" + (10000 + random.nextInt(90000));
    }

    /**
     * Generates a counterparty based on venue.
     */
    private String generateCounterparty(String venue) {
        return switch (venue) {
            case "LSE" -> "BARCLAYS-" + random.nextInt(100);
            case "NYSE", "NASDAQ" -> "GOLDMAN-" + random.nextInt(100);
            case "TSE" -> "NOMURA-" + random.nextInt(100);
            case "SSE" -> "CITIC-" + random.nextInt(100);
            default -> "BROKER-" + random.nextInt(1000);
        };
    }

    /**
     * Generates a regulatory ID based on region.
     */
    private String generateRegulatoryId(String region) {
        String prefix = switch (region) {
            case "UK" -> "MIFID-";
            case "US" -> "SEC-";
            case "JP" -> "FSA-";
            case "CN" -> "CSRC-";
            default -> "REG-";
        };
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Generates a trader ID.
     */
    private String generateTraderId() {
        String[] prefixes = {"TRD", "ALG", "HFT", "INS", "RET"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return prefix + "-" + (1000 + random.nextInt(9000));
    }

    /**
     * Rounds a double to 2 decimal places.
     */
    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Rounds a double to 5 decimal places (for FX rates).
     */
    private double roundToFiveDecimalPlaces(double value) {
        return Math.round(value * 100000.0) / 100000.0;
    }
}