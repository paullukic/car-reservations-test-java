package com.reservation.car.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.reservation.car.entity.Reservation;
import com.reservation.car.model.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Reservation entities.
 * Includes only necessary fields for API consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDTO {
    
    private UUID id;
    private UUID carId;
    private UUID userId;
    private Instant startTime;
    private Instant endTime;
    private ReservationStatus status;
    private Instant createdAt;
    
    /**
     * Converts Reservation entity to response DTO.
     */
    public static ReservationResponseDTO from(Reservation reservation) {
        return new ReservationResponseDTO(
            reservation.getId(),
            reservation.getCarId(),
            reservation.getUserId(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getStatus(),
            reservation.getCreatedAt()
        );
    }
}