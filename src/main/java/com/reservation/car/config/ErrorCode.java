package com.reservation.car.config;

/**
 * Centralized error codes and messages for consistent error handling across the application.
 * Supports internationalization and reduces code duplication.
 */
public enum ErrorCode {
    CAR_UNAVAILABLE("CAR_UNAVAILABLE", "Car unavailable"),
    CAR_NOT_FOUND("CAR_NOT_FOUND", "Car not found"),
    INVALID_RESERVATION("INVALID_RESERVATION", "Invalid reservation"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Request validation failed"),
    INVALID_ARGUMENT("INVALID_ARGUMENT", "Invalid argument"),
    MISSING_PARAMETER("MISSING_PARAMETER", "Required parameter '%s' is missing"),
    MISSING_HEADER("MISSING_HEADER", "Required header '%s' is missing"),
    INVALID_PARAMETER_TYPE("INVALID_PARAMETER_TYPE", "Parameter '%s' has invalid format"),
    DATABASE_ERROR("DATABASE_ERROR", "Database temporarily unavailable. Please try again later."),
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred. Please try again later.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}