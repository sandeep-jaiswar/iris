package org.jaiswarsecurities.replayengine.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.replayengine.config.ReplayProperties;
import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;
import org.jaiswarsecurities.replayengine.model.ReplayCheckpoint;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Orchestrates the replay process with different speed modes.
 * Handles reading events, scheduling their replay, and managing checkpoints.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayScheduler {
    
    private final ChipmunkReader chipmunkReader;
    private final KafkaPublisher kafkaPublisher;
    private final CheckpointManager checkpointManager;
    private final ReplayProperties replayProperties;
    private final MeterRegistry meterRegistry;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsFailed = new AtomicLong(0);
    private ScheduledExecutorService checkpointExecutor;
    
    private volatile Instant replayStartTime;
    private volatile Instant lastEventTime;
    private volatile String currentFileId;
    
    private volatile boolean metricsInitialized = false;
    
    @PostConstruct
    public void initMetrics() {
        if (metricsInitialized) {
            log.debug("Metrics already initialized, skipping");
            return;
        }
        
        try {
            Gauge.builder("replay.events.processed", this, ReplayScheduler::getEventsProcessed)
                    .description("Number of events processed during current replay")
                    .register(meterRegistry);
                    
            Gauge.builder("replay.events.failed", this, ReplayScheduler::getEventsFailed)
                    .description("Number of events that failed during current replay")
                    .register(meterRegistry);
                    
            Gauge.builder("replay.is.running", this, scheduler -> scheduler.isRunning() ? 1.0 : 0.0)
                    .description("Whether replay is currently running (1=running, 0=stopped)")
                    .register(meterRegistry);
                    
            metricsInitialized = true;
            log.debug("Metrics initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize metrics, continuing without metrics", e);
            // Don't throw the exception to prevent application startup failure
        }
    }
    
    /**
     * Starts the replay process.
     * 
     * @return A future that completes when the replay finishes or fails
     */
    public CompletableFuture<Void> startReplay() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Replay is already running");
        }
        
        log.info("Starting replay with mode: {}", replayProperties.getSpeedMode());
        
        replayStartTime = Instant.now();
        currentFileId = generateFileIdentifier();
        
        // Reset counters
        eventsProcessed.set(0);
        eventsFailed.set(0);
        
        // Start checkpoint scheduler
        startCheckpointScheduler();
        
        try {
            // Load existing checkpoint if available
            ReplayCheckpoint existingCheckpoint = checkpointManager.loadCheckpoint(currentFileId);
            if (existingCheckpoint != null) {
                log.info("Resuming replay from checkpoint: line={}, events={}", 
                        existingCheckpoint.getCurrentLineNumber(), existingCheckpoint.getEventsPublished());
            }
            
            return switch (replayProperties.getSpeedMode()) {
                case REAL_TIME -> startRealTimeReplay(existingCheckpoint);
                case ACCELERATED -> startAcceleratedReplay(existingCheckpoint);
                case BURST -> startBurstReplay(existingCheckpoint);
            };
            
        } catch (Exception e) {
            isRunning.set(false);
            stopCheckpointScheduler();
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Stops the replay process.
     */
    public void stopReplay() {
        if (!isRunning.compareAndSet(true, false)) {
            log.warn("Replay is not currently running");
            return;
        }
        
        log.info("Stopping replay...");
        stopCheckpointScheduler();
        kafkaPublisher.close();
        
        try {
            chipmunkReader.close();
        } catch (Exception e) {
            log.warn("Error closing chipmunk reader", e);
        }
    }
    
    private CompletableFuture<Void> startRealTimeReplay(ReplayCheckpoint checkpoint) {
        return CompletableFuture.runAsync(() -> {
            try (Stream<ChipmunkEvent> events = chipmunkReader.readEvents()) {
                
                long skipCount = checkpoint != null ? checkpoint.getCurrentLineNumber() : 0;
                AtomicReference<Instant> firstEventTimeRef = new AtomicReference<>();
                
                events.skip(skipCount)
                        .forEach(event -> {
                            if (!isRunning.get()) {
                                return;
                            }
                            
                            try {
                                // Calculate delay to maintain real-time spacing
                                Instant firstEventTime = firstEventTimeRef.get();
                                if (firstEventTime == null) {
                                    firstEventTime = event.getTimestamp();
                                    firstEventTimeRef.set(firstEventTime);
                                }
                                
                                Duration eventDelay = Duration.between(firstEventTime, event.getTimestamp());
                                Duration elapsedReplay = Duration.between(replayStartTime, Instant.now());
                                
                                if (eventDelay.compareTo(elapsedReplay) > 0) {
                                    long sleepMs = eventDelay.minus(elapsedReplay).toMillis();
                                    Thread.sleep(Math.max(0, sleepMs));
                                }
                                
                                publishEvent(event);
                                
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.info("Real-time replay interrupted");
                                return;
                            } catch (Exception e) {
                                log.error("Error in real-time replay", e);
                                eventsFailed.incrementAndGet();
                            }
                        });
                        
                completeReplay();
                
            } catch (Exception e) {
                log.error("Real-time replay failed", e);
                throw new RuntimeException("Real-time replay failed", e);
            }
        });
    }
    
    private CompletableFuture<Void> startAcceleratedReplay(ReplayCheckpoint checkpoint) {
        return CompletableFuture.runAsync(() -> {
            try (Stream<ChipmunkEvent> events = chipmunkReader.readEvents()) {
                
                long skipCount = checkpoint != null ? checkpoint.getCurrentLineNumber() : 0;
                AtomicReference<Instant> firstEventTimeRef = new AtomicReference<>();
                
                events.skip(skipCount)
                        .forEach(event -> {
                            if (!isRunning.get()) {
                                return;
                            }
                            
                            try {
                                // Calculate accelerated delay
                                Instant firstEventTime = firstEventTimeRef.get();
                                if (firstEventTime == null) {
                                    firstEventTime = event.getTimestamp();
                                    firstEventTimeRef.set(firstEventTime);
                                }
                                
                                Duration eventDelay = Duration.between(firstEventTime, event.getTimestamp());
                                Duration acceleratedDelay = eventDelay.dividedBy((long) replayProperties.getSpeedMultiplier());
                                Duration elapsedReplay = Duration.between(replayStartTime, Instant.now());
                                
                                if (acceleratedDelay.compareTo(elapsedReplay) > 0) {
                                    long sleepMs = acceleratedDelay.minus(elapsedReplay).toMillis();
                                    Thread.sleep(Math.max(0, sleepMs));
                                }
                                
                                publishEvent(event);
                                
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.info("Accelerated replay interrupted");
                                return;
                            } catch (Exception e) {
                                log.error("Error in accelerated replay", e);
                                eventsFailed.incrementAndGet();
                            }
                        });
                        
                completeReplay();
                
            } catch (Exception e) {
                log.error("Accelerated replay failed", e);
                throw new RuntimeException("Accelerated replay failed", e);
            }
        });
    }
    
    private CompletableFuture<Void> startBurstReplay(ReplayCheckpoint checkpoint) {
        return CompletableFuture.runAsync(() -> {
            try (Stream<ChipmunkEvent> events = chipmunkReader.readEvents()) {
                
                long skipCount = checkpoint != null ? checkpoint.getCurrentLineNumber() : 0;
                
                events.skip(skipCount)
                        .forEach(event -> {
                            if (!isRunning.get()) {
                                return;
                            }
                            
                            try {
                                publishEvent(event);
                                
                                // Add small delay every batch to prevent overwhelming Kafka
                                if (eventsProcessed.get() % replayProperties.getBurstBatchSize() == 0) {
                                    Thread.sleep(10); // 10ms pause between batches
                                }
                                
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.info("Burst replay interrupted");
                                return;
                            } catch (Exception e) {
                                log.error("Error in burst replay", e);
                                eventsFailed.incrementAndGet();
                            }
                        });
                        
                completeReplay();
                
            } catch (Exception e) {
                log.error("Burst replay failed", e);
                throw new RuntimeException("Burst replay failed", e);
            }
        });
    }
    
    private void publishEvent(ChipmunkEvent event) {
        kafkaPublisher.publishEvent(event)
                .whenComplete((metadata, exception) -> {
                    if (exception == null) {
                        eventsProcessed.incrementAndGet();
                        lastEventTime = event.getTimestamp();
                    } else {
                        eventsFailed.incrementAndGet();
                        log.warn("Failed to publish event: {}", event.getEventId(), exception);
                    }
                });
    }
    
    private void completeReplay() {
        log.info("Replay completed successfully. Events processed: {}, failed: {}", 
                eventsProcessed.get(), eventsFailed.get());
        
        // Delete checkpoint since replay completed successfully
        checkpointManager.deleteCheckpoint(currentFileId);
        
        stopReplay();
    }
    
    private void startCheckpointScheduler() {
        checkpointExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "checkpoint-scheduler");
            t.setDaemon(true);
            return t;
        });
        
        int intervalSeconds = replayProperties.getCheckpoint().getIntervalSeconds();
        checkpointExecutor.scheduleAtFixedRate(this::createCheckpoint, 
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
    
    private void stopCheckpointScheduler() {
        if (checkpointExecutor != null) {
            checkpointExecutor.shutdown();
            try {
                if (!checkpointExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    checkpointExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                checkpointExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void createCheckpoint() {
        if (!isRunning.get()) {
            return;
        }
        
        ReplayCheckpoint checkpoint = ReplayCheckpoint.builder()
                .fileIdentifier(currentFileId)
                .currentLineNumber(eventsProcessed.get())
                .checkpointTime(Instant.now())
                .eventsPublished(eventsProcessed.get())
                .eventsFailed(eventsFailed.get())
                .lastEventTime(lastEventTime)
                .speedMode(replayProperties.getSpeedMode().getValue())
                .build();
                
        checkpointManager.saveCheckpoint(checkpoint);
    }
    
    private String generateFileIdentifier() {
        // Generate a unique identifier for this replay session
        if (replayProperties.getSource().getType() == ReplayProperties.Source.SourceType.MINIO) {
            return replayProperties.getSource().getMinio().getBucketName() + "/" + 
                   replayProperties.getSource().getMinio().getObjectKey();
        } else {
            return replayProperties.getSource().getLocal().getFilePath();
        }
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public long getEventsProcessed() {
        return eventsProcessed.get();
    }
    
    public long getEventsFailed() {
        return eventsFailed.get();
    }
}