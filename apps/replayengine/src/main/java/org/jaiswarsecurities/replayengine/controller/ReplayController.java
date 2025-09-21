package org.jaiswarsecurities.replayengine.controller;

import lombok.RequiredArgsConstructor;
import org.jaiswarsecurities.replayengine.model.ReplayCheckpoint;
import org.jaiswarsecurities.replayengine.service.CheckpointManager;
import org.jaiswarsecurities.replayengine.service.ReplayScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for managing and monitoring the replay engine.
 */
@RestController
@RequestMapping("/api/replay")
@RequiredArgsConstructor
public class ReplayController {
    
    private final ReplayScheduler replayScheduler;
    private final CheckpointManager checkpointManager;
    
    /**
     * Starts the replay process.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startReplay() {
        try {
            if (replayScheduler.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Replay is already running"));
            }
            
            CompletableFuture<Void> future = replayScheduler.startReplay();
            
            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Replay started successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to start replay: " + e.getMessage()));
        }
    }
    
    /**
     * Stops the replay process.
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopReplay() {
        try {
            if (!replayScheduler.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Replay is not currently running"));
            }
            
            replayScheduler.stopReplay();
            
            return ResponseEntity.ok(Map.of(
                    "status", "stopped",
                    "message", "Replay stopped successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to stop replay: " + e.getMessage()));
        }
    }
    
    /**
     * Gets the current status of the replay process.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        ReplayCheckpoint checkpoint = checkpointManager.getCurrentCheckpoint();
        
        return ResponseEntity.ok(Map.of(
                "isRunning", replayScheduler.isRunning(),
                "eventsProcessed", replayScheduler.getEventsProcessed(),
                "eventsFailed", replayScheduler.getEventsFailed(),
                "currentCheckpoint", checkpoint != null ? checkpoint : "No checkpoint available"
        ));
    }
    
    /**
     * Gets health information about the replay engine.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Replay Engine",
                "version", "1.0.0"
        ));
    }
}