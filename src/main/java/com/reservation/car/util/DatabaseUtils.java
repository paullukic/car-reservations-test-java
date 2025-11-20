package com.reservation.car.util;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Utility class for database-related operations and exception handling.
 */
public class DatabaseUtils {

    /**
     * Checks if the DataIntegrityViolationException is caused by a PostgreSQL exclusion constraint violation.
     * Exclusion constraint violations contain specific text in the error message.
     *
     * @param e the DataIntegrityViolationException to check
     * @return true if it's an exclusion constraint violation, false otherwise
     */
    public static boolean isExclusionConstraintViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        return message != null && message.contains("conflicting key value violates exclusion constraint");
    }

    private DatabaseUtils() {
        // Utility class
    }
}