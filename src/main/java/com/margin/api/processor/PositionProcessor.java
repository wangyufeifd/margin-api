package com.margin.api.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.cache.FIFOQueue;
import com.margin.api.model.Execution;
import com.margin.api.model.Position;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Processor that transforms executions into positions
 */
@Singleton
public class PositionProcessor implements Processor<Position> {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionProcessor.class);
    
    private final Vertx vertx;
    private final FIFOQueue<Position> cache;

    @Inject
    public PositionProcessor(
            Vertx vertx,
            @Named("positionQueueSize") int queueSize) {
        this.vertx = vertx;
        this.cache = new FIFOQueue<>(queueSize);
        logger.info("PositionProcessor initialized with queue size: {}", queueSize);
    }

    @Override
    public Future<Position> process(Execution execution) {
        return vertx.executeBlocking(promise -> {
            try {
                logger.debug("Processing execution to position: {}", execution.getId());
                
                // Determine position side based on execution side
                Position.PositionSide side = execution.getSide() == Execution.ExecutionSide.BUY 
                        ? Position.PositionSide.LONG 
                        : Position.PositionSide.SHORT;
                
                // For simplicity, treating each execution as a new position
                // In a real system, you'd aggregate with existing positions
                BigDecimal quantity = execution.getSide() == Execution.ExecutionSide.BUY
                        ? execution.getQuantity()
                        : execution.getQuantity().negate();
                
                Position position = new Position(
                        UUID.randomUUID().toString(),
                        execution.getAccountId(),
                        execution.getSymbol(),
                        quantity,
                        execution.getPrice(),
                        BigDecimal.ZERO, // unrealized PnL starts at 0
                        BigDecimal.ZERO, // realized PnL starts at 0
                        side,
                        Instant.now(),
                        Instant.now()
                );
                
                // Add to cache
                cache.offer(position);
                logger.debug("Position added to cache: {} (queue size: {})", position.getId(), cache.size());
                
                promise.complete(position);
            } catch (Exception e) {
                logger.error("Error processing execution to position", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public FIFOQueue<Position> getCache() {
        return cache;
    }

    @Override
    public String getProcessorType() {
        return "POSITION";
    }
}

