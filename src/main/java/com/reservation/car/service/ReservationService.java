package com.reservation.car.service;

import java.util.UUID;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.exception.CarNotFoundException;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.exception.InvalidReservationException;

/**
 * Service interface for managing car reservations.
 * Handles creation, cancellation, and validation with concurrency protection.
 */
public interface ReservationService {

    /**
     * Creates a new car reservation with full validation and concurrency protection.
     *
     * @param request the reservation request details
     * @param requestingUserId the ID of the user making the request (for authorization)
     * @return the created reservation DTO
     * @throws CarNotFoundException if car doesn't exist
     * @throws CarUnavailableException if time slot is unavailable
     * @throws InvalidReservationException if request violates business rules
     */
    ReservationResponseDTO createReservation(ReservationRequestDTO request, UUID requestingUserId);

    /**
     * Cancels an existing reservation if cancellation rules allow it.
     *
     * @param reservationId the reservation to cancel
     * @param userId the user requesting cancellation (for authorization)
     * @return the cancelled reservation DTO
     * @throws InvalidReservationException if cancellation is not allowed
     */
    ReservationResponseDTO cancelReservation(UUID reservationId, UUID userId);
}