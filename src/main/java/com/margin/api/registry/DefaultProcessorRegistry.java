package com.margin.api.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.margin.api.model.Execution;
import com.margin.api.processor.Processor;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ProcessorRegistry
 * Routes executions to all registered processors in parallel
 */
@Singleton
public class DefaultProcessorRegistry implements ProcessorRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultProcessorRegistry.class);
    
    private final Vertx vertx;
    private final Map<String, Processor<?>> processors;
    private final List<Processor<?>> processorList;

    @Inject
    public DefaultProcessorRegistry(Vertx vertx) {
        this.vertx = vertx;
        this.processors = new ConcurrentHashMap<>();
        this.processorList = new ArrayList<>();
        logger.info("ProcessorRegistry initialized");
    }

    @Override
    public void register(Processor<?> processor) {
        String type = processor.getProcessorType();
        processors.put(type, processor);
        processorList.add(processor);
        logger.info("Registered processor: {}", type);
    }

    @Override
    public Future<Void> process(Execution execution) {
        if (processorList.isEmpty()) {
            return Future.succeededFuture();
        }

        logger.debug("Processing execution {} through {} processors", 
                execution.getId(), processorList.size());

        // Process execution through all processors in parallel
        List<Future> futures = new ArrayList<>();
        for (Processor<?> processor : processorList) {
            Future<?> future = processor.process(execution)
                .onSuccess(result -> 
                    logger.debug("Processor {} completed for execution {}", 
                        processor.getProcessorType(), execution.getId()))
                .onFailure(err -> 
                    logger.error("Processor {} failed for execution {}", 
                        processor.getProcessorType(), execution.getId(), err));
            futures.add(future);
        }

        // Wait for all processors to complete
        return CompositeFuture.all(futures).mapEmpty();
    }

    @Override
    public List<Processor<?>> getProcessors() {
        return new ArrayList<>(processorList);
    }

    @Override
    public Processor<?> getProcessorByType(String type) {
        return processors.get(type);
    }
}

