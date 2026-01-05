package com.margin.api.refdata;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of RefDataService
 * Uses in-memory cache with default values (prototype implementation)
 */
@Singleton
public class DefaultRefDataService implements RefDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultRefDataService.class);
    
    // Default rates
    private static final BigDecimal DEFAULT_INITIAL_MARGIN_RATE = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_MAINTENANCE_MARGIN_RATE = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_LEVERAGE = new BigDecimal("2.0");
    
    // In-memory cache (prototype - would be replaced with actual service calls)
    private final Map<String, MarginRate> marginRateCache = new ConcurrentHashMap<>();
    private final Map<String, MarketData> marketDataCache = new ConcurrentHashMap<>();
    private final Map<String, AccountRiskCategory> riskCategoryCache = new ConcurrentHashMap<>();

    public DefaultRefDataService() {
        logger.info("DefaultRefDataService initialized");
    }

    @Override
    public MarginRate getMarginRate(String symbol) throws RefDataException {
        try {
            return marginRateCache.computeIfAbsent(symbol, s -> {
                logger.debug("Loading margin rate for symbol: {}", s);
                return new MarginRate(
                    DEFAULT_INITIAL_MARGIN_RATE,
                    DEFAULT_MAINTENANCE_MARGIN_RATE,
                    DEFAULT_LEVERAGE
                );
            });
        } catch (Exception e) {
            throw new RefDataException(
                "Failed to get margin rate for symbol: " + symbol,
                symbol,
                RefDataException.ErrorType.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    @Override
    public MarketData getMarketData(String symbol) throws RefDataException {
        try {
            return marketDataCache.computeIfAbsent(symbol, s -> {
                logger.debug("Loading market data for symbol: {}", s);
                // Prototype: return dummy data
                return new MarketData(s, new BigDecimal("100.00"), System.currentTimeMillis());
            });
        } catch (Exception e) {
            throw new RefDataException(
                "Failed to get market data for symbol: " + symbol,
                symbol,
                RefDataException.ErrorType.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    @Override
    public AccountRiskCategory getAccountRiskCategory(String account) throws RefDataException {
        try {
            return riskCategoryCache.computeIfAbsent(account, a -> {
                logger.debug("Loading risk category for account: {}", a);
                return new AccountRiskCategory(a, RiskTier.MEDIUM, new BigDecimal("5.0"));
            });
        } catch (Exception e) {
            throw new RefDataException(
                "Failed to get risk category for account: " + account,
                account,
                RefDataException.ErrorType.SERVICE_UNAVAILABLE,
                e
            );
        }
    }
}

