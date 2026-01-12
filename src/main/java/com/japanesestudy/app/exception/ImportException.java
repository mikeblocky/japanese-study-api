package com.japanesestudy.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception for import-related failures (Anki deck parsing, file handling, etc.)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ImportException extends RuntimeException {
    
    private final String errorType;
    private final String details;
    
    public ImportException(String message) {
        super(message);
        this.errorType = "ImportError";
        this.details = null;
    }
    
    public ImportException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
        this.details = null;
    }
    
    public ImportException(String message, String errorType, String details) {
        super(message);
        this.errorType = errorType;
        this.details = details;
    }
    
    public ImportException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = cause.getClass().getSimpleName();
        this.details = cause.getMessage();
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public String getDetails() {
        return details;
    }
}
