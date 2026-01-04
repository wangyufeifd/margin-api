package com.margin.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.margin.api.loader.KafkaDataLoader;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Margin API
 * 
 * NEW ARCHITECTURE (Streamlined Reactive):
 * Kafka → DataLoader → ProcessorRegistry → Processors (parallel) → Aggregators (push) → Cache
 * 
 * Key improvements:
 * - Removed EventBus (unnecessary hop)
 * - Removed Consumer layer (processors self-register)
 * - Removed FIFO Queue polling (push-based aggregation)
 * - Added Caffeine cache (bounded, TTL)
 * - Added RefDataService (enrichment)
 * - Added ProcessorRegistry (plugin architecture)
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("=== Starting Margin API Application ===");
        logger.info("Architecture: Kafka → DataLoader → ProcessorRegistry → Processors → Aggregators → Cache");
        
        // Initialize Guice injector
        Injector injector = Guice.createInjector(new ApplicationModule());
        
        // Get Vertx instance from injector (single instance)
        Vertx vertx = injector.getInstance(Vertx.class);
        
        // Deploy the main verticle
        MainVerticle mainVerticle = injector.getInstance(MainVerticle.class);
        vertx.deployVerticle(mainVerticle, result -> {
            if (result.succeeded()) {
                logger.info("✓ MainVerticle deployed: {}", result.result());
                
                // Start the Kafka data loader (direct to ProcessorRegistry)
                KafkaDataLoader kafkaDataLoader = injector.getInstance(KafkaDataLoader.class);
                kafkaDataLoader.start();
                
                logger.info("=== Margin API Application started successfully ===");
                logger.info("HTTP Server: http://localhost:8080");
                logger.info("Pipeline: Active and ready to process executions");
                logger.info("Processors registered: MarginProcessor, PositionProcessor");
                
            } else {
                logger.error("✗ Failed to deploy verticle", result.cause());
                vertx.close();
            }
        });
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Margin API Application...");
            
            KafkaDataLoader kafkaDataLoader = injector.getInstance(KafkaDataLoader.class);
            kafkaDataLoader.stop();
            
            vertx.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("✓ Application shut down successfully");
                } else {
                    logger.error("✗ Error during shutdown", ar.cause());
                }
            });
        }));
    }
}

