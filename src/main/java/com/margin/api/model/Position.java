package com.margin.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Position model representing a trading position derived from executions
 */
public class Position extends BaseEntity {
    
    private final String accountId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal averagePrice;
    private final BigDecimal unrealizedPnl;
    private final BigDecimal realizedPnl;
    private final PositionSide side;

    @JsonCreator
    public Position(
            @JsonProperty("id") String id,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("averagePrice") BigDecimal averagePrice,
            @JsonProperty("unrealizedPnl") BigDecimal unrealizedPnl,
            @JsonProperty("realizedPnl") BigDecimal realizedPnl,
            @JsonProperty("side") PositionSide side,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {
        super(id, createdAt, updatedAt);
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.averagePrice = averagePrice != null ? averagePrice : BigDecimal.ZERO;
        this.unrealizedPnl = unrealizedPnl != null ? unrealizedPnl : BigDecimal.ZERO;
        this.realizedPnl = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        this.side = side;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public PositionSide getSide() {
        return side;
    }

    public BigDecimal getNotionalValue() {
        return quantity.multiply(averagePrice);
    }

    public BigDecimal getTotalPnl() {
        return unrealizedPnl.add(realizedPnl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Position position = (Position) o;
        return Objects.equals(accountId, position.accountId) &&
                Objects.equals(symbol, position.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountId, symbol);
    }

    @Override
    public String toString() {
        return "Position{" +
                "id='" + getId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", averagePrice=" + averagePrice +
                ", unrealizedPnl=" + unrealizedPnl +
                ", realizedPnl=" + realizedPnl +
                ", side=" + side +
                '}';
    }

    public enum PositionSide {
        LONG,
        SHORT,
        FLAT
    }
}

