package com.margin.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.margin.api.aggregator.MarginAggregator;
import com.margin.api.aggregator.PositionAggregator;
import com.margin.api.consumer.ExecutionConsumer;
import com.margin.api.loader.KafkaDataLoader;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Margin API
 * 
 * Pipeline: Kafka → DataLoader → EventBus → Consumer → Processor → FIFO Queue → Aggregator
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting Margin API Application with Vert.x and Guice...");
        logger.info("Pipeline: Kafka → DataLoader → EventBus → Consumer → Processor → FIFO Queue → Aggregator");
        
        // Initialize Guice injector
        Injector injector = Guice.createInjector(new ApplicationModule());
        
        // Create Vert.x instance
        VertxOptions options = new VertxOptions();
        Vertx vertx = Vertx.vertx(options);
        
        // Deploy the main verticle
        MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);
        vertx.deployVerticle(mainVerticle, result -> {
            if (result.succeeded()) {
                logger.info("MainVerticle deployed successfully: {}", result.result());
                
                // Start the execution consumer (EventBus consumer)
                ExecutionConsumer executionConsumer = injector.getInstance(ExecutionConsumer.class);
                executionConsumer.start();
                
                // Start the aggregators (poll FIFO queues and aggregate to output cache)
                MarginAggregator marginAggregator = injector.getInstance(MarginAggregator.class);
                marginAggregator.start();
                
                PositionAggregator positionAggregator = injector.getInstance(PositionAggregator.class);
                positionAggregator.start();
                
                // Start the Kafka data loader (loads from Kafka and publishes to EventBus)
                KafkaDataLoader kafkaDataLoader = injector.getInstance(KafkaDataLoader.class);
                kafkaDataLoader.start();
                
                logger.info("=== Margin API Application started successfully ===");
                logger.info("HTTP Server: http://localhost:8080");
                logger.info("Data Pipeline: Active and ready to process executions");
                
            } else {
                logger.error("Failed to deploy verticle", result.cause());
                vertx.close();
            }
        });
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Margin API Application...");
            
            KafkaDataLoader kafkaDataLoader = injector.getInstance(KafkaDataLoader.class);
            kafkaDataLoader.stop();
            
            MarginAggregator marginAggregator = injector.getInstance(MarginAggregator.class);
            marginAggregator.stop();
            
            PositionAggregator positionAggregator = injector.getInstance(PositionAggregator.class);
            positionAggregator.stop();
            
            vertx.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("Application shut down successfully");
                } else {
                    logger.error("Error during shutdown", ar.cause());
                }
            });
        }));
    }
}

