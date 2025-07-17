package com.metrics.exportservice.exception;

import java.util.List;

public class BulkProcessingException extends RuntimeException {
    
    private final List<String> details;
    
    public BulkProcessingException(String message) {
        super(message);
        this.details = null;
    }
    
    public BulkProcessingException(String message, List<String> details) {
        super(message);
        this.details = details;
    }
    
    public BulkProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.details = null;
    }
    
    public List<String> getDetails() {
        return details;
    }
}
