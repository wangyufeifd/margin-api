package com.margin.api.aggregator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.margin.api.model.Margin;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Push-based margin aggregator with bounded Caffeine cache
 * Aggregates margin data in real-time as it's pushed from processors
 */
@Singleton
public class MarginAggregator implements Aggregator<Margin, AggregatedMargin> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginAggregator.class);
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final int TTL_HOURS = 24;
    
    private final Vertx vertx;
    private final Cache<String, AggregatedMargin> cache;

    @Inject
    public MarginAggregator(Vertx vertx) {
        this.vertx = vertx;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
                .recordStats()
                .build();
        logger.info("MarginAggregator initialized with Caffeine cache (max={}, TTL={}h)", 
                MAX_CACHE_SIZE, TTL_HOURS);
    }

    @Override
    public Future<AggregatedMargin> add(Margin margin) {
        return vertx.executeBlocking(promise -> {
            try {
                String key = generateKey(margin.getAccountId(), margin.getSymbol());
                
                // Get or create aggregated margin
                AggregatedMargin aggregated = cache.get(key, k -> 
                    new AggregatedMargin(margin.getAccountId(), margin.getSymbol())
                );
                
                // Add margin to aggregated state
                aggregated.addMargin(margin);
                
                // Update cache
                cache.put(key, aggregated);
                
                logger.debug("Added margin to aggregation for key {}: total={}, count={}", 
                        key, aggregated.getTotalMarginRequirement(), aggregated.getCount());
                
                promise.complete(aggregated);
            } catch (Exception e) {
                logger.error("Error adding margin to aggregation", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public AggregatedMargin get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public Map<String, AggregatedMargin> getAll() {
        return new HashMap<>(cache.asMap());
    }

    @Override
    public String getAggregatorType() {
        return "MARGIN";
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


