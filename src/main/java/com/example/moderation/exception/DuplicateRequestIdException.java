package com.example.moderation.exception;

/**
 * Exception được throw khi request ID đã tồn tại trong database.
 * Trả về HTTP 400 Bad Request với message rõ ràng.
 */
public class DuplicateRequestIdException extends RuntimeException {

    private final String requestId;

    public DuplicateRequestIdException(String requestId) {
        super(String.format("Request ID '%s' đã tồn tại trong hệ thống", requestId));
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
