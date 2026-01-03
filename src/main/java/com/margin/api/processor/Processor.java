package com.margin.api.processor;

import com.margin.api.cache.FIFOQueue;
import io.vertx.core.Future;

/**
 * Processor interface for transforming executions into different data types
 */
public interface Processor<T> {
    
    /**
     * Process an execution and transform it to the target type
     * 
     * @param execution The execution to process
     * @return Future containing the transformed result
     */
    Future<T> process(com.margin.api.model.Execution execution);
    
    /**
     * Get the FIFO queue cache for this processor
     * 
     * @return The FIFO queue containing processed items
     */
    FIFOQueue<T> getCache();
    
    /**
     * Get the processor type identifier
     * 
     * @return String identifier for this processor
     */
    String getProcessorType();
}

