package com.example.moderation.exception;

/**
 * Exception cho lỗi khi gọi external API (Alibaba Cloud).
 * Chứa thông tin chi tiết về lỗi từ third-party service.
 */
public class ExternalApiException extends RuntimeException {

    private final String apiName;
    private final Integer statusCode;
    private final String apiErrorMessage;

    public ExternalApiException(String apiName, String message) {
        super(String.format("Lỗi khi gọi API %s: %s", apiName, message));
        this.apiName = apiName;
        this.statusCode = null;
        this.apiErrorMessage = message;
    }

    public ExternalApiException(String apiName, Integer statusCode, String message) {
        super(String.format("Lỗi khi gọi API %s (Status: %d): %s", apiName, statusCode, message));
        this.apiName = apiName;
        this.statusCode = statusCode;
        this.apiErrorMessage = message;
    }

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(String.format("Lỗi khi gọi API %s: %s", apiName, message), cause);
        this.apiName = apiName;
        this.statusCode = null;
        this.apiErrorMessage = message;
    }

    public String getApiName() {
        return apiName;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getApiErrorMessage() {
        return apiErrorMessage;
    }
}
