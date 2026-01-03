package com.margin.api;

import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Verticle that sets up the HTTP server and routes
 */
public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    
    private final JsonObject config;

    @Inject
    public MainVerticle(JsonObject config) {
        this.config = config;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        
        // Add body handler for POST/PUT requests
        router.route().handler(BodyHandler.create());
        
        // Setup routes
        setupRoutes(router);
        
        // Create HTTP server
        int port = config.getInteger("http.port", 8080);
        String host = config.getString("http.host", "0.0.0.0");
        
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router)
            .listen(port, host, result -> {
                if (result.succeeded()) {
                    logger.info("HTTP server started on {}:{}", host, port);
                    startPromise.complete();
                } else {
                    logger.error("Failed to start HTTP server", result.cause());
                    startPromise.fail(result.cause());
                }
            });
    }

    private void setupRoutes(Router router) {
        // Health check endpoint
        router.get("/health").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("status", "UP")
                    .put("service", "margin-api")
                    .put("pipeline", "Kafka → DataLoader → EventBus → Consumer → Processor → FIFO → Aggregator")
                    .encode());
        });

        // Root endpoint
        router.get("/").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("message", "Welcome to Margin API")
                    .put("version", "1.0.0-SNAPSHOT")
                    .put("description", "Trade Execution Processing Pipeline")
                    .encode());
        });
        
        // API documentation endpoint
        router.get("/api/info").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("pipeline", new JsonObject()
                        .put("1", "Kafka DataLoader - Loads trade executions from Kafka")
                        .put("2", "EventBus - Vert.x event bus for message passing")
                        .put("3", "Consumer - Reads executions and distributes to processors")
                        .put("4", "Processors - Transform executions (Margin, Position)")
                        .put("5", "FIFO Queue - Cache for transformed data")
                        .put("6", "Aggregators - Aggregate data to output cache"))
                    .put("endpoints", new JsonObject()
                        .put("GET /", "Welcome message")
                        .put("GET /health", "Health check")
                        .put("GET /api/info", "API information"))
                    .encode());
        });
    }
}

