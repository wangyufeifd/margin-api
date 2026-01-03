package com.margin.api.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.cache.FIFOQueue;
import com.margin.api.model.Execution;
import com.margin.api.model.Margin;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Processor that transforms executions into margin requirements
 */
@Singleton
public class MarginProcessor implements Processor<Margin> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginProcessor.class);
    private static final BigDecimal INITIAL_MARGIN_RATE = new BigDecimal("0.50"); // 50%
    private static final BigDecimal MAINTENANCE_MARGIN_RATE = new BigDecimal("0.25"); // 25%
    private static final BigDecimal DEFAULT_LEVERAGE = new BigDecimal("2.0");
    
    private final Vertx vertx;
    private final FIFOQueue<Margin> cache;

    @Inject
    public MarginProcessor(
            Vertx vertx,
            @Named("marginQueueSize") int queueSize) {
        this.vertx = vertx;
        this.cache = new FIFOQueue<>(queueSize);
        logger.info("MarginProcessor initialized with queue size: {}", queueSize);
    }

    @Override
    public Future<Margin> process(Execution execution) {
        return vertx.executeBlocking(promise -> {
            try {
                logger.debug("Processing execution to margin: {}", execution.getId());
                
                BigDecimal notionalValue = execution.getNotionalValue();
                BigDecimal initialMargin = notionalValue.multiply(INITIAL_MARGIN_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal maintenanceMargin = notionalValue.multiply(MAINTENANCE_MARGIN_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                
                Margin margin = new Margin(
                        UUID.randomUUID().toString(),
                        execution.getAccountId(),
                        execution.getId(),
                        execution.getSymbol(),
                        initialMargin,
                        maintenanceMargin,
                        initialMargin, // margin requirement = initial margin
                        DEFAULT_LEVERAGE,
                        Instant.now(),
                        Instant.now()
                );
                
                // Add to cache
                cache.offer(margin);
                logger.debug("Margin added to cache: {} (queue size: {})", margin.getId(), cache.size());
                
                promise.complete(margin);
            } catch (Exception e) {
                logger.error("Error processing execution to margin", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public FIFOQueue<Margin> getCache() {
        return cache;
    }

    @Override
    public String getProcessorType() {
        return "MARGIN";
    }
}

