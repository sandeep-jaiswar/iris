package org.jaiswarsecurities.replayengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * IRIS Replay Engine Application
 * 
 * Provides high-volume, low-latency replay of Chipmunk files into Kafka topics.
 * Supports MinIO integration, configurable replay speeds, checkpointing, and metrics.
 * 
 * Features:
 * - Reads Chipmunk files from MinIO or local filesystem
 * - Publishes events to Kafka topics (trade-events, market-data, fx-rates)  
 * - Configurable replay modes: real-time, accelerated, burst
 * - Crash recovery via checkpointing
 * - Prometheus metrics endpoint at /actuator/prometheus
 * - REST API for control and monitoring
 * 
 * Configuration: replay-config.yaml
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan("org.jaiswarsecurities.replayengine.config")
public class ReplayEngineApplication {

    public static void main(String[] args) {
        log.info("Starting IRIS Replay Engine Application...");
        SpringApplication.run(ReplayEngineApplication.class, args);
        log.info("IRIS Replay Engine Application started successfully!");
    }
}
