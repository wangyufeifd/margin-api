package com.margin.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Margin model representing margin requirement from an execution
 */
public class Margin extends BaseEntity {
    
    private final String accountId;
    private final String executionId;
    private final String symbol;
    private final BigDecimal initialMargin;
    private final BigDecimal maintenanceMargin;
    private final BigDecimal marginRequirement;
    private final BigDecimal leverage;

    @JsonCreator
    public Margin(
            @JsonProperty("id") String id,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("executionId") String executionId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("initialMargin") BigDecimal initialMargin,
            @JsonProperty("maintenanceMargin") BigDecimal maintenanceMargin,
            @JsonProperty("marginRequirement") BigDecimal marginRequirement,
            @JsonProperty("leverage") BigDecimal leverage,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {
        super(id, createdAt, updatedAt);
        this.accountId = accountId;
        this.executionId = executionId;
        this.symbol = symbol;
        this.initialMargin = initialMargin != null ? initialMargin : BigDecimal.ZERO;
        this.maintenanceMargin = maintenanceMargin != null ? maintenanceMargin : BigDecimal.ZERO;
        this.marginRequirement = marginRequirement != null ? marginRequirement : BigDecimal.ZERO;
        this.leverage = leverage != null ? leverage : BigDecimal.ONE;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getInitialMargin() {
        return initialMargin;
    }

    public BigDecimal getMaintenanceMargin() {
        return maintenanceMargin;
    }

    public BigDecimal getMarginRequirement() {
        return marginRequirement;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Margin margin = (Margin) o;
        return Objects.equals(executionId, margin.executionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), executionId);
    }

    @Override
    public String toString() {
        return "Margin{" +
                "id='" + getId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", initialMargin=" + initialMargin +
                ", maintenanceMargin=" + maintenanceMargin +
                ", marginRequirement=" + marginRequirement +
                ", leverage=" + leverage +
                '}';
    }
}

