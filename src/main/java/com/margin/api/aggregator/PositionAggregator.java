package com.margin.api.aggregator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.model.Position;
import com.margin.api.processor.PositionProcessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator that aggregates position data from the FIFO queue
 * Maintains aggregated positions by account and symbol
 */
@Singleton
public class PositionAggregator implements Aggregator<Position, AggregatedPosition> {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionAggregator.class);
    
    private final Vertx vertx;
    private final PositionProcessor positionProcessor;
    private final Map<String, AggregatedPosition> outputCache;
    private final long pollInterval;
    private Long timerId;

    @Inject
    public PositionAggregator(
            Vertx vertx,
            PositionProcessor positionProcessor,
            @Named("aggregatorPollInterval") long pollInterval) {
        this.vertx = vertx;
        this.positionProcessor = positionProcessor;
        this.outputCache = new ConcurrentHashMap<>();
        this.pollInterval = pollInterval;
        logger.info("PositionAggregator initialized with poll interval: {}ms", pollInterval);
    }

    @Override
    public Future<AggregatedPosition> aggregate(Position position) {
        return vertx.executeBlocking(promise -> {
            try {
                String key = generateKey(position.getAccountId(), position.getSymbol());
                
                AggregatedPosition aggregated = outputCache.computeIfAbsent(key, k -> 
                    new AggregatedPosition(position.getAccountId(), position.getSymbol())
                );
                
                // Aggregate the position
                aggregated.addPosition(position);
                
                logger.debug("Aggregated position for key {}: quantity={}, avgPrice={}", 
                        key, aggregated.getNetQuantity(), aggregated.getAveragePrice());
                
                promise.complete(aggregated);
            } catch (Exception e) {
                logger.error("Error aggregating position", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public AggregatedPosition get(String key) {
        return outputCache.get(key);
    }

    @Override
    public String getAggregatorType() {
        return "POSITION";
    }

    @Override
    public void start() {
        logger.info("Starting PositionAggregator polling...");
        timerId = vertx.setPeriodic(pollInterval, id -> {
            positionProcessor.getCache().drain(position -> {
                aggregate(position).onFailure(err -> 
                    logger.error("Failed to aggregate position: {}", position.getId(), err)
                );
            });
        });
    }

    @Override
    public void stop() {
        if (timerId != null) {
            logger.info("Stopping PositionAggregator polling...");
            vertx.cancelTimer(timerId);
            timerId = null;
        }
    }

    private String generateKey(String accountId, String symbol) {
        return accountId + ":" + symbol;
    }

    public Map<String, AggregatedPosition> getOutputCache() {
        return outputCache;
    }
}

