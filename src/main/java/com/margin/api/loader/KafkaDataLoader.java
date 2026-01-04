package com.margin.api.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.margin.api.model.Execution;
import com.margin.api.model.TradeExecutionWrapper;
import com.margin.api.registry.ProcessorRegistry;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DataLoader that loads trade executions from Kafka
 * Directly routes to ProcessorRegistry (no EventBus)
 * Simplified RocksDB support (prototype - would be enhanced in production)
 */
@Singleton
public class KafkaDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaDataLoader.class);
    
    private final Vertx vertx;
    private final ProcessorRegistry processorRegistry;
    private final String bootstrapServers;
    private final String groupId;
    private final Set<String> topics;
    private final ObjectMapper objectMapper;
    private KafkaConsumer<String, String> consumer;
    private volatile boolean paused = false;

    @Inject
    public KafkaDataLoader(
            Vertx vertx,
            ProcessorRegistry processorRegistry,
            @Named("kafka.bootstrap.servers") String bootstrapServers,
            @Named("kafka.group.id") String groupId,
            @Named("kafka.topics") Set<String> topics) {
        this.vertx = vertx;
        this.processorRegistry = processorRegistry;
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topics = topics;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        logger.info("KafkaDataLoader initialized for topics: {} on servers: {}", topics, bootstrapServers);
    }

    /**
     * Start consuming from Kafka and routing to ProcessorRegistry
     */
    public void start() {
        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // Manual commit for reliability
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500"); // Batch size

        consumer = KafkaConsumer.create(vertx, config);

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
                
                // TODO: Persist to RocksDB for crash recovery (prototype skips for simplicity)
                // String key = record.topic() + ":" + record.partition() + ":" + record.offset();
                // rocksDB.put(key.getBytes(), value.getBytes());
                
                // Route directly to ProcessorRegistry (no EventBus)
                logger.debug("Routing execution {} to ProcessorRegistry", execution.getId());
                
                processorRegistry.process(execution)
                    .onSuccess(v -> {
                        logger.info("Successfully processed execution {} through all processors", execution.getId());
                        // Commit offset after successful processing
                        consumer.commit(ar -> {
                            if (ar.failed()) {
                                logger.error("Failed to commit offset for execution {}", execution.getId(), ar.cause());
                            }
                        });
                    })
                    .onFailure(err -> {
                        logger.error("Failed to process execution {} through processors", execution.getId(), err);
                        // TODO: Send to DLQ
                    });
                
            } catch (Exception e) {
                logger.error("Error processing Kafka record from topic: {}", record.topic(), e);
                // TODO: Send to DLQ
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

    /**
     * Pause consumption (for backpressure)
     */
    public void pause() {
        if (consumer != null && !paused) {
            consumer.pause();
            paused = true;
            logger.warn("Kafka consumption PAUSED due to backpressure");
        }
    }

    /**
     * Resume consumption (after backpressure relief)
     */
    public void resume() {
        if (consumer != null && paused) {
            consumer.resume();
            paused = false;
            logger.info("Kafka consumption RESUMED");
        }
    }

    public boolean isPaused() {
        return paused;
    }
}

