package org.jaiswarsecurities.replayengine.service;

import org.jaiswarsecurities.replayengine.model.ChipmunkEvent;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Interface for reading Chipmunk files from different sources.
 * Implementations should handle reading from MinIO, local files, etc.
 */
public interface ChipmunkReader {
    
    /**
     * Opens a stream of events from the configured source.
     * The stream should preserve the order of events and provide lazy loading.
     * 
     * @return A stream of ChipmunkEvent objects
     * @throws IOException if there's an error reading the source
     */
    Stream<ChipmunkEvent> readEvents() throws IOException;
    
    /**
     * Gets the total number of lines/events in the source (if available).
     * Returns -1 if the count cannot be determined without reading the entire file.
     * 
     * @return Total number of events, or -1 if unknown
     * @throws IOException if there's an error accessing the source
     */
    long getTotalEventCount() throws IOException;
    
    /**
     * Closes any resources associated with the reader.
     */
    void close() throws IOException;
}