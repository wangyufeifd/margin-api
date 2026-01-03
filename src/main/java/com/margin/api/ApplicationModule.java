package com.margin.api;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.Set;

/**
 * Guice module for dependency injection configuration
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        // Processors and Aggregators are bound automatically via @Singleton annotations
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
                .put("kafka.topics", "trade-executions")
                .put("margin.queue.size", 1000)
                .put("position.queue.size", 1000)
                .put("aggregator.poll.interval", 1000);
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

    @Provides
    @Singleton
    @Named("marginQueueSize")
    public int provideMarginQueueSize(JsonObject config) {
        return config.getInteger("margin.queue.size", 1000);
    }

    @Provides
    @Singleton
    @Named("positionQueueSize")
    public int providePositionQueueSize(JsonObject config) {
        return config.getInteger("position.queue.size", 1000);
    }

    @Provides
    @Singleton
    @Named("aggregatorPollInterval")
    public long provideAggregatorPollInterval(JsonObject config) {
        return config.getLong("aggregator.poll.interval", 1000L);
    }
}

