package com.margin.api.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.margin.api.model.Execution;
import com.margin.api.processor.MarginProcessor;
import com.margin.api.processor.PositionProcessor;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer that reads executions from EventBus and passes them to processors
 */
@Singleton
public class ExecutionConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionConsumer.class);
    private static final String EXECUTION_ADDRESS = "execution.incoming";
    
    private final Vertx vertx;
    private final MarginProcessor marginProcessor;
    private final PositionProcessor positionProcessor;

    @Inject
    public ExecutionConsumer(
            Vertx vertx,
            MarginProcessor marginProcessor,
            PositionProcessor positionProcessor) {
        this.vertx = vertx;
        this.marginProcessor = marginProcessor;
        this.positionProcessor = positionProcessor;
    }

    /**
     * Start consuming execution messages from the EventBus
     */
    public void start() {
        EventBus eventBus = vertx.eventBus();
        
        eventBus.<JsonObject>consumer(EXECUTION_ADDRESS, message -> {
            try {
                JsonObject body = message.body();
                logger.debug("Received execution from EventBus: {}", body.encode());
                
                // Deserialize execution
                Execution execution = body.mapTo(Execution.class);
                
                // Process with margin processor
                marginProcessor.process(execution)
                    .onSuccess(margin -> 
                        logger.debug("Margin processed: {} for execution: {}", 
                                margin.getId(), execution.getId()))
                    .onFailure(err -> 
                        logger.error("Failed to process margin for execution: {}", 
                                execution.getId(), err));
                
                // Process with position processor
                positionProcessor.process(execution)
                    .onSuccess(position -> 
                        logger.debug("Position processed: {} for execution: {}", 
                                position.getId(), execution.getId()))
                    .onFailure(err -> 
                        logger.error("Failed to process position for execution: {}", 
                                execution.getId(), err));
                
                // Reply to acknowledge processing started
                message.reply(new JsonObject()
                    .put("status", "processing")
                    .put("executionId", execution.getId()));
                
            } catch (Exception e) {
                logger.error("Error consuming execution message", e);
                message.fail(500, e.getMessage());
            }
        });
        
        logger.info("ExecutionConsumer started, listening on address: {}", EXECUTION_ADDRESS);
    }

    public static String getExecutionAddress() {
        return EXECUTION_ADDRESS;
    }
}

