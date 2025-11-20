package com.reservation.car.exception;

/**
 * Exception thrown when reservation data violates business rules
 * (duration, timing, cancellation constraints, etc.)
 */
public class InvalidReservationException extends RuntimeException {
    
    public InvalidReservationException(String message) {
        super(message);
    }
    
    public InvalidReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}