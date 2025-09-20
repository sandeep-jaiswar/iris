package org.jaiswarsecurities.replayengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.replayengine.config.ReplayProperties;
import org.jaiswarsecurities.replayengine.model.ReplayCheckpoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Manages checkpointing for replay operations to enable crash recovery.
 * Supports both file-based and DynamoDB-based checkpoint storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckpointManager {
    
    private final ReplayProperties replayProperties;
    private final ObjectMapper objectMapper;
    
    private volatile ReplayCheckpoint currentCheckpoint;
    
    /**
     * Saves the current checkpoint asynchronously.
     * 
     * @param checkpoint The checkpoint to save
     * @return A future that completes when the checkpoint is saved
     */
    @Async
    public CompletableFuture<Void> saveCheckpoint(ReplayCheckpoint checkpoint) {
        if (!replayProperties.getCheckpoint().isEnabled()) {
            log.debug("Checkpointing is disabled, skipping save");
            return CompletableFuture.completedFuture(null);
        }
        
        this.currentCheckpoint = checkpoint;
        
        try {
            switch (replayProperties.getCheckpoint().getStorageType()) {
                case FILE:
                    saveCheckpointToFile(checkpoint);
                    break;
                case DYNAMODB:
                    saveCheckpointToDynamoDB(checkpoint);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown checkpoint storage type: " + 
                            replayProperties.getCheckpoint().getStorageType());
            }
            
            log.debug("Checkpoint saved successfully: line={}, events={}", 
                    checkpoint.getCurrentLineNumber(), checkpoint.getEventsPublished());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to save checkpoint", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Loads the most recent checkpoint for the given file.
     * 
     * @param fileIdentifier Identifier for the file being replayed
     * @return The loaded checkpoint, or null if no checkpoint exists
     */
    public ReplayCheckpoint loadCheckpoint(String fileIdentifier) {
        if (!replayProperties.getCheckpoint().isEnabled()) {
            log.debug("Checkpointing is disabled, returning null checkpoint");
            return null;
        }
        
        try {
            switch (replayProperties.getCheckpoint().getStorageType()) {
                case FILE:
                    return loadCheckpointFromFile(fileIdentifier);
                case DYNAMODB:
                    return loadCheckpointFromDynamoDB(fileIdentifier);
                default:
                    throw new IllegalArgumentException("Unknown checkpoint storage type: " + 
                            replayProperties.getCheckpoint().getStorageType());
            }
            
        } catch (Exception e) {
            log.warn("Failed to load checkpoint for file: {}", fileIdentifier, e);
            return null;
        }
    }
    
    /**
     * Gets the current checkpoint (may be null).
     */
    public ReplayCheckpoint getCurrentCheckpoint() {
        return currentCheckpoint;
    }
    
    /**
     * Deletes the checkpoint for the given file (typically called when replay completes successfully).
     * 
     * @param fileIdentifier Identifier for the file being replayed
     */
    public void deleteCheckpoint(String fileIdentifier) {
        if (!replayProperties.getCheckpoint().isEnabled()) {
            return;
        }
        
        try {
            switch (replayProperties.getCheckpoint().getStorageType()) {
                case FILE:
                    deleteCheckpointFile(fileIdentifier);
                    break;
                case DYNAMODB:
                    deleteCheckpointFromDynamoDB(fileIdentifier);
                    break;
            }
            
            log.info("Checkpoint deleted for file: {}", fileIdentifier);
            
        } catch (Exception e) {
            log.warn("Failed to delete checkpoint for file: {}", fileIdentifier, e);
        }
    }
    
    private void saveCheckpointToFile(ReplayCheckpoint checkpoint) throws IOException {
        Path checkpointPath = Paths.get(replayProperties.getCheckpoint().getFilePath());
        
        // Ensure parent directory exists
        Files.createDirectories(checkpointPath.getParent());
        
        String json = objectMapper.writeValueAsString(checkpoint);
        Files.writeString(checkpointPath, json);
    }
    
    private ReplayCheckpoint loadCheckpointFromFile(String fileIdentifier) throws IOException {
        Path checkpointPath = Paths.get(replayProperties.getCheckpoint().getFilePath());
        
        if (!Files.exists(checkpointPath)) {
            return null;
        }
        
        String json = Files.readString(checkpointPath);
        ReplayCheckpoint checkpoint = objectMapper.readValue(json, ReplayCheckpoint.class);
        
        // Verify the checkpoint is for the correct file
        if (!fileIdentifier.equals(checkpoint.getFileIdentifier())) {
            log.warn("Checkpoint file identifier mismatch: expected={}, found={}", 
                    fileIdentifier, checkpoint.getFileIdentifier());
            return null;
        }
        
        return checkpoint;
    }
    
    private void deleteCheckpointFile(String fileIdentifier) throws IOException {
        Path checkpointPath = Paths.get(replayProperties.getCheckpoint().getFilePath());
        Files.deleteIfExists(checkpointPath);
    }
    
    private void saveCheckpointToDynamoDB(ReplayCheckpoint checkpoint) {
        // TODO: Implement DynamoDB checkpoint storage
        // This would use the DynamoDB client from awsconfig to store checkpoints
        // in a table with the file identifier as the partition key
        log.warn("DynamoDB checkpoint storage not yet implemented");
        throw new UnsupportedOperationException("DynamoDB checkpoint storage not yet implemented");
    }
    
    private ReplayCheckpoint loadCheckpointFromDynamoDB(String fileIdentifier) {
        // TODO: Implement DynamoDB checkpoint loading
        log.warn("DynamoDB checkpoint loading not yet implemented");
        return null;
    }
    
    private void deleteCheckpointFromDynamoDB(String fileIdentifier) {
        // TODO: Implement DynamoDB checkpoint deletion
        log.warn("DynamoDB checkpoint deletion not yet implemented");
    }
}