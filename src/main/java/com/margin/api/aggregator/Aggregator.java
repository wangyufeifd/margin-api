package com.margin.api.aggregator;

import io.vertx.core.Future;

import java.util.Map;

/**
 * Aggregator interface for push-based real-time aggregation
 * No polling - items are pushed directly from processors
 */
public interface Aggregator<T, R> {
    
    /**
     * Add/aggregate an item immediately (push-based)
     * 
     * @param item The item to aggregate
     * @return Future containing the aggregated result
     */
    Future<R> add(T item);
    
    /**
     * Get aggregated data by key
     * 
     * @param key The key to lookup
     * @return The aggregated result or null if not found
     */
    R get(String key);
    
    /**
     * Get all aggregated data
     * 
     * @return Map of all aggregated results
     */
    Map<String, R> getAll();
    
    /**
     * Get the aggregator type identifier
     * 
     * @return String identifier for this aggregator
     */
    String getAggregatorType();
    
    /**
     * Get cache statistics
     * 
     * @return Cache stats (hit rate, size, etc.)
     */
    CacheStats getStats();
    
    /**
     * Cache statistics
     */
    class CacheStats {
        public final long size;
        public final long hitCount;
        public final long missCount;
        public final double hitRate;

        public CacheStats(long size, long hitCount, long missCount, double hitRate) {
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
        }
    }
}

