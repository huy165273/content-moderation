package com.example.moderation.exception;

/**
 * Exception được throw khi không tìm thấy entity trong database.
 * Ví dụ: không tìm thấy TestRun với runId được yêu cầu.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final String identifier;

    public EntityNotFoundException(String entityName, String identifier) {
        super(String.format("%s không tồn tại với ID: %s", entityName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.identifier = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
