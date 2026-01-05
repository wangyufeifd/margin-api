package com.margin.api.aggregator;

import com.margin.api.model.Margin;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregated margin data for an account and symbol
 */
public class AggregatedMargin {
    
    private final String accountId;
    private final String symbol;
    private final AtomicReference<BigDecimal> totalInitialMargin;
    private final AtomicReference<BigDecimal> totalMaintenanceMargin;
    private final AtomicReference<BigDecimal> totalMarginRequirement;
    private final AtomicInteger count;
    private volatile long lastUpdated;

    public AggregatedMargin(String accountId, String symbol) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.totalInitialMargin = new AtomicReference<>(BigDecimal.ZERO);
        this.totalMaintenanceMargin = new AtomicReference<>(BigDecimal.ZERO);
        this.totalMarginRequirement = new AtomicReference<>(BigDecimal.ZERO);
        this.count = new AtomicInteger(0);
        this.lastUpdated = System.currentTimeMillis();
    }

    public void addMargin(Margin margin) {
        totalInitialMargin.updateAndGet(current -> current.add(margin.getInitialMargin()));
        totalMaintenanceMargin.updateAndGet(current -> current.add(margin.getMaintenanceMargin()));
        totalMarginRequirement.updateAndGet(current -> current.add(margin.getMarginRequirement()));
        count.incrementAndGet();
        lastUpdated = System.currentTimeMillis();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getTotalInitialMargin() {
        return totalInitialMargin.get();
    }

    public BigDecimal getTotalMaintenanceMargin() {
        return totalMaintenanceMargin.get();
    }

    public BigDecimal getTotalMarginRequirement() {
        return totalMarginRequirement.get();
    }

    public int getCount() {
        return count.get();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "AggregatedMargin{" +
                "accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", totalInitialMargin=" + totalInitialMargin.get() +
                ", totalMaintenanceMargin=" + totalMaintenanceMargin.get() +
                ", totalMarginRequirement=" + totalMarginRequirement.get() +
                ", count=" + count.get() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

