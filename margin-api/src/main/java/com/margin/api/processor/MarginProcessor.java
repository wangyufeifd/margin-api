package com.margin.api.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.margin.api.aggregator.MarginAggregator;
import com.margin.api.model.Execution;
import com.margin.api.model.Margin;
import com.margin.api.refdata.RefDataException;
import com.margin.api.refdata.RefDataService;
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
 * Pushes results directly to MarginAggregator (no queue)
 */
@Singleton
public class MarginProcessor implements Processor<Margin> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginProcessor.class);
    private static final BigDecimal DEFAULT_INITIAL_MARGIN_RATE = new BigDecimal("0.50"); // 50%
    private static final BigDecimal DEFAULT_MAINTENANCE_MARGIN_RATE = new BigDecimal("0.25"); // 25%
    private static final BigDecimal DEFAULT_LEVERAGE = new BigDecimal("2.0");
    
    private final Vertx vertx;
    private final MarginAggregator aggregator;
    private final RefDataService refDataService;

    @Inject
    public MarginProcessor(
            Vertx vertx,
            MarginAggregator aggregator,
            RefDataService refDataService) {
        this.vertx = vertx;
        this.aggregator = aggregator;
        this.refDataService = refDataService;
        logger.info("MarginProcessor initialized with push-based aggregation");
    }

    @Override
    public Future<Margin> process(Execution execution) {
        return vertx.executeBlocking(promise -> {
            try {
                logger.debug("Processing execution to margin: {}", execution.getId());
                
                // Try to get symbol-specific margin rates from ref data
                BigDecimal initialMarginRate = DEFAULT_INITIAL_MARGIN_RATE;
                BigDecimal maintenanceMarginRate = DEFAULT_MAINTENANCE_MARGIN_RATE;
                BigDecimal leverage = DEFAULT_LEVERAGE;
                
                try {
                    RefDataService.MarginRate marginRate = refDataService.getMarginRate(execution.getSymbol());
                    initialMarginRate = marginRate.getInitialMarginRate();
                    maintenanceMarginRate = marginRate.getMaintenanceMarginRate();
                    leverage = marginRate.getLeverage();
                } catch (RefDataException e) {
                    logger.warn("Failed to get margin rate for {}, using defaults: {}", 
                            execution.getSymbol(), e.getMessage());
                }
                
                BigDecimal notionalValue = execution.getNotionalValue();
                BigDecimal initialMargin = notionalValue.multiply(initialMarginRate)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal maintenanceMargin = notionalValue.multiply(maintenanceMarginRate)
                        .setScale(2, RoundingMode.HALF_UP);
                
                Margin margin = new Margin(
                        UUID.randomUUID().toString(),
                        execution.getAccountId(),
                        execution.getId(),
                        execution.getSymbol(),
                        initialMargin,
                        maintenanceMargin,
                        initialMargin, // margin requirement = initial margin
                        leverage,
                        Instant.now(),
                        Instant.now()
                );
                
                // Push directly to aggregator (no queue)
                aggregator.add(margin)
                    .onSuccess(agg -> logger.debug("Margin pushed to aggregator: {}", margin.getId()))
                    .onFailure(err -> logger.error("Failed to push margin to aggregator", err));
                
                promise.complete(margin);
            } catch (Exception e) {
                logger.error("Error processing execution to margin", e);
                promise.fail(e);
            }
        });
    }

    @Override
    public String getProcessorType() {
        return "MARGIN";
    }
}

