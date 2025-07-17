package com.metrics.exportservice.exception;

public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String entityType, Object id) {
        super(String.format("%s with id '%s' not found", entityType, id));
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
