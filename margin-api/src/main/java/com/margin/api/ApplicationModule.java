package com.margin.api;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.processor.MarginProcessor;
import com.margin.api.processor.PositionProcessor;
import com.margin.api.refdata.DefaultRefDataService;
import com.margin.api.refdata.RefDataService;
import com.margin.api.registry.DefaultProcessorRegistry;
import com.margin.api.registry.ProcessorRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.Set;

/**
 * Guice module for dependency injection configuration
 * Wires the new streamlined architecture:
 * Kafka → DataLoader → ProcessorRegistry → Processors → Aggregators → Cache
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind interfaces to implementations
        bind(ProcessorRegistry.class).to(DefaultProcessorRegistry.class);
        bind(RefDataService.class).to(DefaultRefDataService.class);
        
        // Processors and Aggregators are @Singleton and auto-bound
    }

    @Provides
    @Singleton
    public Vertx provideVertx() {
        return Vertx.vertx();
    }

    @Provides
    @Singleton
    public WebClient provideWebClient(Vertx vertx) {
        return WebClient.create(vertx);
    }

    @Provides
    @Singleton
    public JsonObject provideConfig() {
        return new JsonObject()
                .put("http.port", 8080)
                .put("http.host", "0.0.0.0")
                .put("kafka.bootstrap.servers", "localhost:9092")
                .put("kafka.group.id", "margin-api-consumer-group")
                .put("kafka.topics", "trade-executions");
    }

    @Provides
    @Singleton
    @Named("kafka.bootstrap.servers")
    public String provideKafkaBootstrapServers(JsonObject config) {
        return config.getString("kafka.bootstrap.servers", "localhost:9092");
    }

    @Provides
    @Singleton
    @Named("kafka.group.id")
    public String provideKafkaGroupId(JsonObject config) {
        return config.getString("kafka.group.id", "margin-api-consumer-group");
    }

    @Provides
    @Singleton
    @Named("kafka.topics")
    public Set<String> provideKafkaTopics(JsonObject config) {
        return Set.of(config.getString("kafka.topics", "trade-executions").split(","));
    }
    
    /**
     * Initialize ProcessorRegistry with all processors
     */
    @Provides
    @Singleton
    public ProcessorRegistry provideInitializedRegistry(
            ProcessorRegistry registry,
            MarginProcessor marginProcessor,
            PositionProcessor positionProcessor) {
        
        // Register all processors
        registry.register(marginProcessor);
        registry.register(positionProcessor);
        
        return registry;
    }
}

