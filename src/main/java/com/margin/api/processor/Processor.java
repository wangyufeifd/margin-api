package com.margin.api.processor;

import com.margin.api.model.Execution;
import com.margin.api.refdata.RefDataException;
import com.margin.api.refdata.RefDataService;
import io.vertx.core.Future;

/**
 * Processor interface for transforming executions into different data types
 * Supports reference data enrichment for enhanced processing
 */
public interface Processor<T> {
    
    /**
     * Process an execution and transform it to the target type
     * 
     * @param execution The execution to process
     * @return Future containing the transformed result
     */
    Future<T> process(Execution execution);
    
    /**
     * Get the processor type identifier
     * 
     * @return String identifier for this processor
     */
    String getProcessorType();
    
    /**
     * Enrich execution with reference data (optional override)
     * Default implementation returns execution unchanged
     * 
     * @param execution The execution to enrich
     * @param refDataService The reference data service
     * @return Enriched execution
     * @throws RefDataException if reference data access fails
     */
    default Execution enrich(Execution execution, RefDataService refDataService) throws RefDataException {
        return execution;
    }
}

