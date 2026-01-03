package com.margin.api.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.consumer.ExecutionConsumer;
import com.margin.api.model.Execution;
import com.margin.api.model.TradeExecutionWrapper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DataLoader that loads trade executions from Kafka and publishes to EventBus
 */
@Singleton
public class KafkaDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaDataLoader.class);
    
    private final Vertx vertx;
    private final String bootstrapServers;
    private final String groupId;
    private final Set<String> topics;
    private final ObjectMapper objectMapper;
    private KafkaConsumer<String, String> consumer;

    @Inject
    public KafkaDataLoader(
            Vertx vertx,
            @Named("kafka.bootstrap.servers") String bootstrapServers,
            @Named("kafka.group.id") String groupId,
            @Named("kafka.topics") Set<String> topics) {
        this.vertx = vertx;
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topics = topics;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        logger.info("KafkaDataLoader initialized for topics: {} on servers: {}", topics, bootstrapServers);
    }

    /**
     * Start consuming from Kafka and publishing to EventBus
     */
    public void start() {
        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = KafkaConsumer.create(vertx, config);
        EventBus eventBus = vertx.eventBus();

        consumer.handler(record -> {
            try {
                logger.debug("Received Kafka record from topic: {}, offset: {}", 
                        record.topic(), record.offset());
                
                // Deserialize the wrapper
                String value = record.value();
                TradeExecutionWrapper wrapper = objectMapper.readValue(value, TradeExecutionWrapper.class);
                
                logger.debug("Deserialized wrapper: messageId={}, offset={}", 
                        wrapper.getMessageId(), wrapper.getOffset());
                
                // Retrieve the execution from the wrapper
                Execution execution = wrapper.getExecution();
                
                if (execution == null) {
                    logger.warn("No execution found in wrapper: {}", wrapper.getMessageId());
                    return;
                }
                
                logger.debug("Publishing execution {} to EventBus", execution.getId());
                
                // Publish execution to EventBus
                JsonObject executionJson = JsonObject.mapFrom(execution);
                eventBus.publish(ExecutionConsumer.getExecutionAddress(), executionJson);
                
                logger.info("Published execution {} from Kafka to EventBus", execution.getId());
                
            } catch (Exception e) {
                logger.error("Error processing Kafka record from topic: {}", record.topic(), e);
            }
        });

        consumer.exceptionHandler(err -> {
            logger.error("Kafka consumer error", err);
        });

        // Subscribe to topics
        consumer.subscribe(topics, ar -> {
            if (ar.succeeded()) {
                logger.info("Kafka consumer subscribed to topics: {}", topics);
            } else {
                logger.error("Failed to subscribe to Kafka topics: {}", topics, ar.cause());
            }
        });
    }

    /**
     * Stop the Kafka consumer
     */
    public void stop() {
        if (consumer != null) {
            logger.info("Stopping Kafka consumer...");
            consumer.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("Kafka consumer closed successfully");
                } else {
                    logger.error("Failed to close Kafka consumer", ar.cause());
                }
            });
        }
    }
}

