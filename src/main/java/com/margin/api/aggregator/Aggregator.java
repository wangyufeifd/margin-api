package com.margin.api.aggregator;

import io.vertx.core.Future;

/**
 * Aggregator interface for aggregating data from FIFO queues to output cache
 */
public interface Aggregator<T, R> {
    
    /**
     * Aggregate an item and update the output cache
     * 
     * @param item The item to aggregate
     * @return Future containing the aggregated result
     */
    Future<R> aggregate(T item);
    
    /**
     * Get aggregated data by key
     * 
     * @param key The key to lookup
     * @return The aggregated result or null if not found
     */
    R get(String key);
    
    /**
     * Get the aggregator type identifier
     * 
     * @return String identifier for this aggregator
     */
    String getAggregatorType();
    
    /**
     * Start the aggregation polling process
     */
    void start();
    
    /**
     * Stop the aggregation polling process
     */
    void stop();
}

