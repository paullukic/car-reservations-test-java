package com.reservation.car.util;

import java.time.Duration;
import java.time.Instant;

import com.reservation.car.config.ErrorCode;
import com.reservation.car.exception.InvalidReservationException;

/**
 * Utility class for validating time slots against business rules.
 * Centralizes time validation logic to avoid duplication across services.
 * 
 * Note: This provides application-level validation with user-friendly error messages.
 * Database constraints provide the same validation as a safety net and ensure
 * data integrity even with direct database access.
 * 
 * Defense-in-depth validation strategy:
 * 1. Application layer - User-friendly error messages, early validation
 * 2. Database layer - Data integrity guarantee, cannot be bypassed
 */
public final class TimeSlotValidator {

    // Prevent instantiation
    private TimeSlotValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Duration MIN_DURATION = Duration.ofHours(2);
    private static final Duration MAX_DURATION = Duration.ofHours(24);

    /**
     * Validates a time slot against all business rules.
     * 
     * @param startTime the start time to validate
     * @param endTime the end time to validate
     * @throws InvalidReservationException if validation fails
     */
    public static void validateTimeSlot(Instant startTime, Instant endTime) {
        Instant now = Instant.now();
        
        if (!startTime.isAfter(now)) {
            throw new InvalidReservationException(ErrorCode.INVALID_RESERVATION.getMessage() + ": Start time must be in the future");
        }
        
        if (!endTime.isAfter(startTime)) {
            throw new InvalidReservationException(ErrorCode.INVALID_RESERVATION.getMessage() + ": End time must be after start time");
        }
        
        Duration duration = Duration.between(startTime, endTime);
        
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw new InvalidReservationException(
                ErrorCode.INVALID_RESERVATION.getMessage() + ": Reservation must be at least 2 hours");
        }
        
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new InvalidReservationException(
                ErrorCode.INVALID_RESERVATION.getMessage() + ": Reservation cannot exceed 24 hours");
        }
    }
}