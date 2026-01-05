package com.margin.api.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * FIFO Queue implementation for caching transformed data
 * Thread-safe implementation using ConcurrentLinkedQueue
 */
public class FIFOQueue<T> {
    
    private final ConcurrentLinkedQueue<T> queue;
    private final int maxSize;
    private final AtomicInteger size;

    public FIFOQueue(int maxSize) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.maxSize = maxSize;
        this.size = new AtomicInteger(0);
    }

    /**
     * Add an item to the queue
     * If queue is at max capacity, removes oldest item first
     */
    public boolean offer(T item) {
        if (item == null) {
            return false;
        }

        // If at max size, remove oldest item
        while (size.get() >= maxSize) {
            poll();
        }

        boolean added = queue.offer(item);
        if (added) {
            size.incrementAndGet();
        }
        return added;
    }

    /**
     * Retrieve and remove the head of the queue
     */
    public Optional<T> poll() {
        T item = queue.poll();
        if (item != null) {
            size.decrementAndGet();
            return Optional.of(item);
        }
        return Optional.empty();
    }

    /**
     * Retrieve but don't remove the head of the queue
     */
    public Optional<T> peek() {
        T item = queue.peek();
        return Optional.ofNullable(item);
    }

    /**
     * Process all items in the queue with the given consumer
     * Items are removed from the queue as they are processed
     */
    public void drain(Consumer<T> consumer) {
        Optional<T> item;
        while ((item = poll()).isPresent()) {
            consumer.accept(item.get());
        }
    }

    /**
     * Get current size of the queue
     */
    public int size() {
        return size.get();
    }

    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }

    /**
     * Check if queue is at max capacity
     */
    public boolean isFull() {
        return size.get() >= maxSize;
    }

    /**
     * Clear all items from the queue
     */
    public void clear() {
        queue.clear();
        size.set(0);
    }

    /**
     * Get max capacity of the queue
     */
    public int getMaxSize() {
        return maxSize;
    }
}

