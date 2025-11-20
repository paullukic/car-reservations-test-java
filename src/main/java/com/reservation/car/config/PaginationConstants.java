package com.reservation.car.config;

/**
 * Application-wide pagination constants.
 * Centralizes pagination configuration for consistent behavior across all paginated endpoints.
 * Optimized for large scale operations.
 */
public final class PaginationConstants {

    // Prevent instantiation
    private PaginationConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Default pagination values
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 50;

    // Pagination limits for performance optimization
    public static final int MAX_PAGE_SIZE = 200;
    public static final int MIN_PAGE_SIZE = 1;

    // String constants for Spring annotations (must be constants for annotation processing)
    public static final String DEFAULT_PAGE_STR = "0";
    public static final String DEFAULT_PAGE_SIZE_STR = "50";

    // Validation error messages
    public static final String PAGE_SIZE_EXCEEDS_MAX_MESSAGE = "Page size cannot exceed 200 for performance reasons";
    public static final String PAGE_NUMBER_NON_NEGATIVE_MESSAGE = "Page number must be non-negative";
    public static final String PAGE_SIZE_MIN_MESSAGE = "Page size must be at least 1";
}