package com.reservation.car.config;

/**
 * API-related constants for versioning and paths.
 */
public final class ApiConstants {
    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String API_V1_BASE = "/api/v1";
    public static final String RESERVATIONS_PATH = API_V1_BASE + "/reservations";
    public static final String CARS_PATH = API_V1_BASE + "/cars";
}