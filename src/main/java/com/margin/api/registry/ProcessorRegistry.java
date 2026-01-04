package com.margin.api.registry;

import com.margin.api.model.Execution;
import com.margin.api.processor.Processor;
import io.vertx.core.Future;

import java.util.List;

/**
 * ProcessorRegistry - Plugin routing hub
 * Manages all business processors and routes executions to appropriate processors
 */
public interface ProcessorRegistry {
    
    /**
     * Register a processor
     * @param processor The processor to register
     */
    void register(Processor<?> processor);
    
    /**
     * Process an execution through all registered processors
     * @param execution The execution to process
     * @return Future that completes when all processors have processed the execution
     */
    Future<Void> process(Execution execution);
    
    /**
     * Get all registered processors
     * @return List of registered processors
     */
    List<Processor<?>> getProcessors();
    
    /**
     * Get processor by type
     * @param type The processor type
     * @return The processor or null if not found
     */
    Processor<?> getProcessorByType(String type);
}

