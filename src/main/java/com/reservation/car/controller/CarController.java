package com.reservation.car.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reservation.car.config.ApiConstants;
import com.reservation.car.config.PaginationConstants;
import com.reservation.car.dto.response.CarResponseDTO;
import com.reservation.car.service.CarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for car-related operations.
 * Handles car listing and availability checking for large fleet scale.
 */
@RestController
@RequestMapping(ApiConstants.CARS_PATH)
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Cars", description = "Car fleet management and availability operations")
public class CarController {

    private final CarService carService;

    /**
     * Retrieves all cars with pagination.
     * Optimized for large fleets performance.
     */
    @Operation(
        summary = "List all cars",
        description = "Retrieves paginated list of all cars in the fleet. Optimized for large fleets."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping
    public ResponseEntity<Page<CarResponseDTO>> getAllCars(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_PAGE_STR) 
            @Min(value = 0, message = PaginationConstants.PAGE_NUMBER_NON_NEGATIVE_MESSAGE) 
            int page,
            
            @Parameter(description = "Page size (max 200 for performance)", example = "50")
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_PAGE_SIZE_STR) 
            @Min(value = PaginationConstants.MIN_PAGE_SIZE, message = PaginationConstants.PAGE_SIZE_MIN_MESSAGE)
            @Max(value = PaginationConstants.MAX_PAGE_SIZE, message = PaginationConstants.PAGE_SIZE_EXCEEDS_MAX_MESSAGE)
            int size) {
        
        log.info("Retrieving all cars - page: {}, size: {}", page, size);
        
        Page<CarResponseDTO> response = carService.getAllCars(page, size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Checks car availability for specific time period.
     */
    @Operation(
        summary = "Check car availability", 
        description = "Returns cars available for booking during the specified time period. Optimized for concurrent access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available cars retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid time parameters or pagination")
    })
    @GetMapping("/available")
    public ResponseEntity<Page<CarResponseDTO>> getAvailableCars(
            @Parameter(description = "Start time (ISO 8601 format)", example = "2025-11-19T15:00:00Z", required = true)
            @RequestParam Instant startTime,
            
            @Parameter(description = "End time (ISO 8601 format)", example = "2025-11-19T19:00:00Z", required = true) 
            @RequestParam Instant endTime,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_PAGE_STR)
            @Min(value = 0, message = PaginationConstants.PAGE_NUMBER_NON_NEGATIVE_MESSAGE)
            int page,
            
            @Parameter(description = "Page size (max 200 for performance)", example = "50")
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_PAGE_SIZE_STR)
            @Min(value = PaginationConstants.MIN_PAGE_SIZE, message = PaginationConstants.PAGE_SIZE_MIN_MESSAGE)
            @Max(value = PaginationConstants.MAX_PAGE_SIZE, message = PaginationConstants.PAGE_SIZE_EXCEEDS_MAX_MESSAGE)
            int size) {
        
        log.info("Checking car availability from {} to {} - page: {}, size: {}", 
            startTime, endTime, page, size);
        
        Page<CarResponseDTO> response = 
            carService.findAvailableCars(startTime, endTime, page, size);
        
        return ResponseEntity.ok(response);
    }
}