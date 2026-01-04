package com.margin.api.aggregator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.margin.api.model.Position;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Push-based position aggregator with bounded Caffeine cache
 * Aggregates position data in real-time as it's pushed from processors
 */
@Singleton
public class PositionAggregator implements Aggregator<Position, AggregatedPosition> {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionAggregator.class);
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final int TTL_HOURS = 24;
    
    private final Vertx vertx;
    private final Cache<String, AggregatedPosition> cache;

    @Inject
    public PositionAggregator(Vertx vertx) {
        this.vertx = vertx;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
                .recordStats()
                .build();
        logger.info("PositionAggregator initialized with Caffeine cache (max={}, TTL={}h)", 
                MAX_CACHE_SIZE, TTL_HOURS);
    }

    @Override
    public Future<AggregatedPosition> add(Position position) {
        return vertx.executeBlocking(promise -> {
            try {
                String key = generateKey(position.getAccountId(), position.getSymbol());
                
                // Get or create aggregated position
                AggregatedPosition aggregated = cache.get(key, k -> 
                    new AggregatedPosition(position.getAccountId(), position.getSymbol())
                );
                
                // Add position to aggregated state
                aggregated.addPosition(position);
                
                // Update cache
                cache.put(key, aggregated);
                
                logger.debug("Added position to aggregation for key {}: quantity={}, avgPrice={}", 
                        key, aggregated.getNetQuantity(), aggregated.getAveragePrice());
                
                promise.complete(aggregated);
            } catch (Exception e) {
                logger.error("Error adding position to aggregation", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public AggregatedPosition get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public Map<String, AggregatedPosition> getAll() {
        return new HashMap<>(cache.asMap());
    }

    @Override
    public String getAggregatorType() {
        return "POSITION";
    }

    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        return new CacheStats(
                cache.estimatedSize(),
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate()
        );
    }

    private String generateKey(String accountId, String symbol) {
        return accountId + ":" + symbol;
    }
}


