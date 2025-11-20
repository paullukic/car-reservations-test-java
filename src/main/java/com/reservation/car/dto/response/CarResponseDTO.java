package com.reservation.car.dto.response;

import java.util.UUID;

import com.reservation.car.entity.Car;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Car entities.
 * Exposes only safe fields for public API consumption.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponseDTO {
    
    private UUID id;
    private String make;
    private String model;
    private String licensePlate;
    
    /**
     * Converts Car entity to response DTO.
     */
    public static CarResponseDTO from(Car car) {
        return new CarResponseDTO(
            car.getId(),
            car.getMake(),
            car.getModel(),
            car.getLicensePlate()
        );
    }
}