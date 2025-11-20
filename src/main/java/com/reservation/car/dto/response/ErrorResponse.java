package com.reservation.car.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized error response for all API endpoints.
 * Provides consistent error structure across the application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String error;
    private String message;
    private int status;
    private Instant timestamp;
    private String path;
    private List<String> details;
    
    public ErrorResponse(String error, String message, int status, String path) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = Instant.now();
    }
    
    public ErrorResponse(String error, String message, int status, String path, List<String> details) {
        this(error, message, status, path);
        this.details = details;
    }
}