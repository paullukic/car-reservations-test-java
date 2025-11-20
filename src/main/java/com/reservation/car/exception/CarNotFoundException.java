package com.reservation.car.exception;

/**
 * Exception thrown when a car with the specified ID does not exist.
 */
public class CarNotFoundException extends RuntimeException {
    
    public CarNotFoundException(String message) {
        super(message);
    }
    
    public CarNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}