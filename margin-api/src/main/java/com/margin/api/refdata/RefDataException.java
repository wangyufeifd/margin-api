package com.margin.api.refdata;

/**
 * Exception thrown when reference data is unavailable or invalid
 */
public class RefDataException extends Exception {
    
    private final String symbol;
    private final ErrorType errorType;

    public RefDataException(String message, String symbol, ErrorType errorType) {
        super(message);
        this.symbol = symbol;
        this.errorType = errorType;
    }

    public RefDataException(String message, String symbol, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.symbol = symbol;
        this.errorType = errorType;
    }

    public String getSymbol() {
        return symbol;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        NOT_FOUND,
        SERVICE_UNAVAILABLE,
        TIMEOUT,
        INVALID_DATA
    }
}

