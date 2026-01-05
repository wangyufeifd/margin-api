package com.margin.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Wrapper class for trade execution data from Kafka
 */
public class TradeExecutionWrapper {
    
    private final String messageId;
    private final String topic;
    private final long offset;
    private final long timestamp;
    private final Execution execution;

    @JsonCreator
    public TradeExecutionWrapper(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("topic") String topic,
            @JsonProperty("offset") long offset,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("execution") Execution execution) {
        this.messageId = messageId;
        this.topic = topic;
        this.offset = offset;
        this.timestamp = timestamp;
        this.execution = execution;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTopic() {
        return topic;
    }

    public long getOffset() {
        return offset;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Execution getExecution() {
        return execution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeExecutionWrapper that = (TradeExecutionWrapper) o;
        return offset == that.offset &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, topic, offset);
    }

    @Override
    public String toString() {
        return "TradeExecutionWrapper{" +
                "messageId='" + messageId + '\'' +
                ", topic='" + topic + '\'' +
                ", offset=" + offset +
                ", timestamp=" + timestamp +
                ", execution=" + execution +
                '}';
    }
}

