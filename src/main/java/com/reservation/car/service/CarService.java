package com.reservation.car.service;

import java.time.Instant;

import org.springframework.data.domain.Page;

import com.reservation.car.dto.response.CarResponseDTO;

/**
 * Service interface for managing car fleet operations.
 * Handles listing and availability checks for large fleets.
 */
public interface CarService {

    /**
     * Retrieves all cars in the system with pagination.
     *
     * @param page page number (0-based)
     * @param size number of cars per page (recommended: 50-200 for UI)
     * @return paginated response with car DTOs and metadata
     */
    Page<CarResponseDTO> getAllCars(int page, int size);

    /**
     * Finds all cars available for booking during the specified time period with pagination.
     *
     * @param startTime the desired start time
     * @param endTime the desired end time
     * @param page page number (0-based)
     * @param size number of cars per page
     * @return paginated response with available car DTOs
     * @throws com.reservation.car.exception.InvalidReservationException if time period is invalid
     */
    Page<CarResponseDTO> findAvailableCars(Instant startTime, Instant endTime, int page, int size);
}