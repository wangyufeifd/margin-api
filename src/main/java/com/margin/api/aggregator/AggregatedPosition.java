package com.margin.api.aggregator;

import com.margin.api.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregated position data for an account and symbol
 */
public class AggregatedPosition {
    
    private final String accountId;
    private final String symbol;
    private final AtomicReference<BigDecimal> netQuantity;
    private final AtomicReference<BigDecimal> totalCost;
    private final AtomicReference<BigDecimal> unrealizedPnl;
    private final AtomicReference<BigDecimal> realizedPnl;
    private final AtomicInteger count;
    private volatile long lastUpdated;

    public AggregatedPosition(String accountId, String symbol) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.netQuantity = new AtomicReference<>(BigDecimal.ZERO);
        this.totalCost = new AtomicReference<>(BigDecimal.ZERO);
        this.unrealizedPnl = new AtomicReference<>(BigDecimal.ZERO);
        this.realizedPnl = new AtomicReference<>(BigDecimal.ZERO);
        this.count = new AtomicInteger(0);
        this.lastUpdated = System.currentTimeMillis();
    }

    public void addPosition(Position position) {
        netQuantity.updateAndGet(current -> current.add(position.getQuantity()));
        totalCost.updateAndGet(current -> 
            current.add(position.getQuantity().multiply(position.getAveragePrice())));
        unrealizedPnl.updateAndGet(current -> current.add(position.getUnrealizedPnl()));
        realizedPnl.updateAndGet(current -> current.add(position.getRealizedPnl()));
        count.incrementAndGet();
        lastUpdated = System.currentTimeMillis();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getNetQuantity() {
        return netQuantity.get();
    }

    public BigDecimal getAveragePrice() {
        BigDecimal qty = netQuantity.get();
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.get().divide(qty, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl.get();
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl.get();
    }

    public BigDecimal getTotalPnl() {
        return unrealizedPnl.get().add(realizedPnl.get());
    }

    public int getCount() {
        return count.get();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public Position.PositionSide getSide() {
        int comparison = netQuantity.get().compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return Position.PositionSide.LONG;
        } else if (comparison < 0) {
            return Position.PositionSide.SHORT;
        } else {
            return Position.PositionSide.FLAT;
        }
    }

    @Override
    public String toString() {
        return "AggregatedPosition{" +
                "accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", netQuantity=" + netQuantity.get() +
                ", averagePrice=" + getAveragePrice() +
                ", unrealizedPnl=" + unrealizedPnl.get() +
                ", realizedPnl=" + realizedPnl.get() +
                ", side=" + getSide() +
                ", count=" + count.get() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

