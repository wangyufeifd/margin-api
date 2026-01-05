package com.margin.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Execution model representing a trade execution
 */
public class Execution extends BaseEntity {
    
    private final String accountId;
    private final String symbol;
    private final String orderId;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final ExecutionSide side;
    private final ExecutionType type;
    private final Instant executionTime;

    @JsonCreator
    public Execution(
            @JsonProperty("id") String id,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("side") ExecutionSide side,
            @JsonProperty("type") ExecutionType type,
            @JsonProperty("executionTime") Instant executionTime,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {
        super(id, createdAt, updatedAt);
        this.accountId = accountId;
        this.symbol = symbol;
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.type = type != null ? type : ExecutionType.MARKET;
        this.executionTime = executionTime != null ? executionTime : Instant.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ExecutionSide getSide() {
        return side;
    }

    public ExecutionType getType() {
        return type;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public BigDecimal getNotionalValue() {
        return price.multiply(quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Execution execution = (Execution) o;
        return Objects.equals(orderId, execution.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId);
    }

    @Override
    public String toString() {
        return "Execution{" +
                "id='" + getId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", orderId='" + orderId + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", side=" + side +
                ", type=" + type +
                ", executionTime=" + executionTime +
                '}';
    }

    public enum ExecutionSide {
        BUY,
        SELL
    }

    public enum ExecutionType {
        MARKET,
        LIMIT,
        STOP,
        STOP_LIMIT
    }
}

