package com.margin.api.aggregator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.model.Margin;
import com.margin.api.processor.MarginProcessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator that aggregates margin data from the FIFO queue
 * Maintains aggregated margin by account and symbol
 */
@Singleton
public class MarginAggregator implements Aggregator<Margin, AggregatedMargin> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginAggregator.class);
    
    private final Vertx vertx;
    private final MarginProcessor marginProcessor;
    private final Map<String, AggregatedMargin> outputCache;
    private final long pollInterval;
    private Long timerId;

    @Inject
    public MarginAggregator(
            Vertx vertx,
            MarginProcessor marginProcessor,
            @Named("aggregatorPollInterval") long pollInterval) {
        this.vertx = vertx;
        this.marginProcessor = marginProcessor;
        this.outputCache = new ConcurrentHashMap<>();
        this.pollInterval = pollInterval;
        logger.info("MarginAggregator initialized with poll interval: {}ms", pollInterval);
    }

    @Override
    public Future<AggregatedMargin> aggregate(Margin margin) {
        return vertx.executeBlocking(promise -> {
            try {
                String key = generateKey(margin.getAccountId(), margin.getSymbol());
                
                AggregatedMargin aggregated = outputCache.computeIfAbsent(key, k -> 
                    new AggregatedMargin(margin.getAccountId(), margin.getSymbol())
                );
                
                // Aggregate the margin values
                aggregated.addMargin(margin);
                
                logger.debug("Aggregated margin for key {}: total={}, count={}", 
                        key, aggregated.getTotalMarginRequirement(), aggregated.getCount());
                
                promise.complete(aggregated);
            } catch (Exception e) {
                logger.error("Error aggregating margin", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public AggregatedMargin get(String key) {
        return outputCache.get(key);
    }

    @Override
    public String getAggregatorType() {
        return "MARGIN";
    }

    @Override
    public void start() {
        logger.info("Starting MarginAggregator polling...");
        timerId = vertx.setPeriodic(pollInterval, id -> {
            marginProcessor.getCache().drain(margin -> {
                aggregate(margin).onFailure(err -> 
                    logger.error("Failed to aggregate margin: {}", margin.getId(), err)
                );
            });
        });
    }

    @Override
    public void stop() {
        if (timerId != null) {
            logger.info("Stopping MarginAggregator polling...");
            vertx.cancelTimer(timerId);
            timerId = null;
        }
    }

    private String generateKey(String accountId, String symbol) {
        return accountId + ":" + symbol;
    }

    public Map<String, AggregatedMargin> getOutputCache() {
        return outputCache;
    }
}

