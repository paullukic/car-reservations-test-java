package com.reservation.car.exception;

/**
 * Exception thrown when a car is not available for reservation.
 * This covers cases like overlapping reservations or car not found.
 */
public class CarUnavailableException extends RuntimeException {

    public CarUnavailableException(String message) {
        super(message);
    }

    public CarUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}