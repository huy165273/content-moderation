package com.example.moderation.provider;

/**
 * Exception cho moderation operations
 */
public class ModerationException extends RuntimeException {

    private final String providerName;
    private final Integer errorCode;

    public ModerationException(String message) {
        super(message);
        this.providerName = null;
        this.errorCode = null;
    }

    public ModerationException(String message, Throwable cause) {
        super(message, cause);
        this.providerName = null;
        this.errorCode = null;
    }

    public ModerationException(String providerName, Integer errorCode, String message) {
        super(message);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public ModerationException(String providerName, Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
