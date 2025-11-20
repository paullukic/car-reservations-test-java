package com.reservation.car.controller;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.reservation.car.dto.response.CarResponseDTO;
import com.reservation.car.exception.InvalidReservationException;
import com.reservation.car.service.CarService;
import com.reservation.car.util.TestDataFactory;

@WebMvcTest(CarController.class)
class CarControllerTest extends BaseControllerTest {

    @MockitoBean
    private CarService carService;

    @Test
    void shouldReturnPagedCars_whenGetAllCars() throws Exception {
        // Arrange
        CarResponseDTO car = TestDataFactory.createCarResponseDTO(testCarId, "Tesla", "Model 3", "ABC-123");
        Page<CarResponseDTO> response = new PageImpl<>(List.of(car), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(carService.getAllCars(0, 50)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].make").value("Tesla"));
    }

    @Test
    void shouldReturnAvailableCars_whenGetAvailableCars() throws Exception {
        // Arrange
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        CarResponseDTO car = TestDataFactory.createCarResponseDTO(testCarId, "Tesla", "Model 3", "ABC-123");
        Page<CarResponseDTO> response = new PageImpl<>(List.of(car), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(carService.findAvailableCars(start, end, 0, 50)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cars/available")
                .param("startTime", start.toString())
                .param("endTime", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].make").value("Tesla"));
    }

    /**
     * Parameterized test to verify that invalid pagination parameters return 400 Bad Request.
     * Tests both /api/v1/cars and /api/v1/cars/available endpoints for consistent validation.
     */
    @ParameterizedTest
    @MethodSource("provideInvalidPaginationParams")
    void shouldReturn400_whenInvalidPaginationParams(String endpoint, String paramName, String paramValue) throws Exception {
        // Act & Assert
        mockMvc.perform(get(endpoint)
                .param(paramName, paramValue))
            .andExpect(status().isBadRequest());
    }

    /**
     * Parameterized test to verify that invalid time parameters for availability return 400 Bad Request.
     * Covers missing times, invalid formats, and logical errors.
     */
    @ParameterizedTest
    @MethodSource("provideInvalidTimeParams")
    void shouldReturn400_whenInvalidTimeParams(String description, String startTime, String endTime) throws Exception {
        // Mock service to throw for invalid time parameters
        when(carService.findAvailableCars(any(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidReservationException("Invalid time parameters"));
        
        // Act & Assert
        var builder = get("/api/v1/cars/available");
        if (startTime != null) builder = builder.param("startTime", startTime);
        if (endTime != null) builder = builder.param("endTime", endTime);
        mockMvc.perform(builder).andExpect(status().isBadRequest());
    }


    private static Stream<Arguments> provideInvalidPaginationParams() {
        return Stream.of(
            Arguments.of("/api/v1/cars", "page", "-1"),  // Negative page
            Arguments.of("/api/v1/cars", "size", "0"),   // Size too small
            Arguments.of("/api/v1/cars", "size", "201"), // Size too large
            Arguments.of("/api/v1/cars/available", "page", "-1"),
            Arguments.of("/api/v1/cars/available", "size", "0"),
            Arguments.of("/api/v1/cars/available", "size", "201")
        );
    }


    private static Stream<Arguments> provideInvalidTimeParams() {
        Instant now = Instant.now();
        return Stream.of(
            Arguments.of("Missing startTime", null, now.toString()),
            Arguments.of("Missing endTime", now.toString(), null),
            Arguments.of("End before start", now.plusSeconds(3600).toString(), now.toString()),
            Arguments.of("Invalid startTime format", "invalid-date", now.toString()),
            Arguments.of("Invalid endTime format", now.toString(), "invalid-date")
        );
    }

}