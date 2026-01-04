package com.margin.api.refdata;

import java.math.BigDecimal;

/**
 * Reference data service interface
 * Provides access to external reference data for enrichment
 */
public interface RefDataService {
    
    /**
     * Get margin rate for a specific symbol
     * @param symbol The symbol to get margin rate for
     * @return MarginRate containing initial and maintenance margin rates
     * @throws RefDataException if reference data is unavailable
     */
    MarginRate getMarginRate(String symbol) throws RefDataException;
    
    /**
     * Get market data (latest price) for a specific symbol
     * @param symbol The symbol to get market data for
     * @return MarketData containing latest price and other market info
     * @throws RefDataException if reference data is unavailable
     */
    MarketData getMarketData(String symbol) throws RefDataException;
    
    /**
     * Get risk category for a specific account
     * @param account The account to get risk category for
     * @return AccountRiskCategory containing risk tier and limits
     * @throws RefDataException if reference data is unavailable
     */
    AccountRiskCategory getAccountRiskCategory(String account) throws RefDataException;
    
    /**
     * Margin rate data
     */
    class MarginRate {
        private final BigDecimal initialMarginRate;
        private final BigDecimal maintenanceMarginRate;
        private final BigDecimal leverage;

        public MarginRate(BigDecimal initialMarginRate, BigDecimal maintenanceMarginRate, BigDecimal leverage) {
            this.initialMarginRate = initialMarginRate;
            this.maintenanceMarginRate = maintenanceMarginRate;
            this.leverage = leverage;
        }

        public BigDecimal getInitialMarginRate() {
            return initialMarginRate;
        }

        public BigDecimal getMaintenanceMarginRate() {
            return maintenanceMarginRate;
        }

        public BigDecimal getLeverage() {
            return leverage;
        }
    }
    
    /**
     * Market data
     */
    class MarketData {
        private final String symbol;
        private final BigDecimal lastPrice;
        private final long timestamp;

        public MarketData(String symbol, BigDecimal lastPrice, long timestamp) {
            this.symbol = symbol;
            this.lastPrice = lastPrice;
            this.timestamp = timestamp;
        }

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getLastPrice() {
            return lastPrice;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Account risk category
     */
    class AccountRiskCategory {
        private final String account;
        private final RiskTier tier;
        private final BigDecimal maxLeverage;

        public AccountRiskCategory(String account, RiskTier tier, BigDecimal maxLeverage) {
            this.account = account;
            this.tier = tier;
            this.maxLeverage = maxLeverage;
        }

        public String getAccount() {
            return account;
        }

        public RiskTier getTier() {
            return tier;
        }

        public BigDecimal getMaxLeverage() {
            return maxLeverage;
        }
    }
    
    enum RiskTier {
        LOW, MEDIUM, HIGH, INSTITUTIONAL
    }
}

