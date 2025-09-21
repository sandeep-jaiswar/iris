package org.jaiswarsecurities.replayengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a checkpoint for replay recovery.
 * Stores the current state of the replay process to enable crash recovery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayCheckpoint {
    
    /**
     * Identifier for the file being replayed
     */
    private String fileIdentifier;
    
    /**
     * Current line number being processed
     */
    private long currentLineNumber;
    
    /**
     * Total lines in the file (if known)
     */
    private long totalLines;
    
    /**
     * Timestamp when the checkpoint was created
     */
    private Instant checkpointTime;
    
    /**
     * Number of events successfully published so far
     */
    private long eventsPublished;
    
    /**
     * Number of events that failed to publish
     */
    private long eventsFailed;
    
    /**
     * Current replay position as percentage (0-100)
     */
    private double progressPercentage;
    
    /**
     * The timestamp of the last event processed
     */
    private Instant lastEventTime;
    
    /**
     * Current replay speed mode
     */
    private String speedMode;
}