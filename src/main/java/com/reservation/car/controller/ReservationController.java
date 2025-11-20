package com.reservation.car.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reservation.car.config.ApiConstants;
import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for reservation operations.
 * Handles reservation creation and cancellation with enterprise-grade error handling.
 */
@RestController
@RequestMapping(ApiConstants.RESERVATIONS_PATH)
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Reservations", description = "Car reservation management operations")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Creates a new car reservation.
     * Implements transactional integrity and concurrency protection.
     */
    @Operation(
        summary = "Create reservation",
        description = "Creates a new car reservation with full validation, concurrency protection, and transactional integrity. "
                    + "Handles overlapping reservation conflicts automatically."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation data (validation errors)"),
        @ApiResponse(responseCode = "409", description = "Car unavailable (overlapping reservation or car not found)")
    })
    @PostMapping
    public ResponseEntity<ReservationResponseDTO> createReservation(
            @Parameter(description = "Reservation details", required = true)
            @Valid @RequestBody ReservationRequestDTO request,
            
            @Parameter(description = "User ID for audit and authorization", required = true)
            @RequestHeader("X-User-ID") UUID requestingUserId) {
        
        log.info("Creating reservation for user {} - car: {}, period: {} to {}", 
            request.getUserId(), request.getCarId(), request.getStartTime(), request.getEndTime());
        
        ReservationResponseDTO response = reservationService.createReservation(request, requestingUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancels an existing reservation.
     * Enforces business rules (30-minute minimum notice) and authorization.
     */
    @Operation(
        summary = "Cancel reservation",
        description = "Cancels an existing reservation if business rules allow (30+ minutes before start time). "
                    + "Only the reservation owner can cancel their reservation. "
                    + "DELETE means \"remove this resource from the collection of active resources.\""
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cancellation not allowed (too late or invalid reservation)"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDTO> cancelReservation(
            @Parameter(description = "Reservation ID to cancel", required = true)
            @PathVariable UUID reservationId,
            
            @Parameter(description = "User ID for authorization", required = true)
            @RequestHeader("X-User-ID") UUID requestingUserId) {
        
        log.info("Cancelling reservation {} for user {}", reservationId, requestingUserId);
        
        ReservationResponseDTO response = reservationService.cancelReservation(reservationId, requestingUserId);
        
        return ResponseEntity.ok(response);
    }
}