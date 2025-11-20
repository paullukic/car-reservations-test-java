package com.reservation.car.controller;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.exception.InvalidReservationException;
import com.reservation.car.model.ReservationStatus;
import com.reservation.car.service.ReservationService;
import com.reservation.car.util.TestDataFactory;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest extends BaseControllerTest {

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void shouldCreateReservation_whenValidRequest() throws Exception {
        // Arrange
        ReservationRequestDTO request = TestDataFactory.createReservationRequestDTO(testCarId, testUserId,
            Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200));
        ReservationResponseDTO response = TestDataFactory.createReservationResponseDTO(testReservationId, testCarId, testUserId,
            request.getStartTime(), request.getEndTime(), ReservationStatus.CONFIRMED, Instant.now());
        when(reservationService.createReservation(any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(testReservationId.toString()));
    }

    @Test
    void shouldReturn409_whenCarUnavailable() throws Exception {
        // Arrange
        ReservationRequestDTO request = TestDataFactory.createReservationRequestDTO(testCarId, testUserId,
            Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200));
        when(reservationService.createReservation(any(), any())).thenThrow(new CarUnavailableException("Unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldCancelReservation_whenValid() throws Exception {
        // Arrange
        ReservationResponseDTO response = TestDataFactory.createReservationResponseDTO(testReservationId, testCarId, testUserId,
            Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), ReservationStatus.CANCELLED, Instant.now());
        when(reservationService.cancelReservation(testReservationId, testUserId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", testReservationId)
                .header("X-User-ID", testUserId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400_whenCancellationTooLate() throws Exception {
        // Arrange
        when(reservationService.cancelReservation(testReservationId, testUserId))
            .thenThrow(new InvalidReservationException("Too late"));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", testReservationId)
                .header("X-User-ID", testUserId.toString()))
            .andExpect(status().isBadRequest());
    }

    /**
     * Parameterized test to verify that invalid reservation requests return 400 Bad Request.
     * This ensures robust error handling for various validation failures (e.g., time order, duration limits)
     * and clear HTTP status codes. By using @MethodSource, we test multiple invalid scenarios
     * efficiently without code duplication, improving test coverage and maintainability.
     */
    @ParameterizedTest
    @MethodSource("provideInvalidReservationRequests")
    void shouldReturn400_whenInvalidReservationRequest(String description, ReservationRequestDTO request) throws Exception {
        // Arrange
        when(reservationService.createReservation(any(), any())).thenThrow(new InvalidReservationException("Invalid request"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> provideInvalidReservationRequests() {
        Instant now = Instant.now();
        return Stream.of(
            Arguments.of("End time before start time", new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(), now.plusSeconds(3600), now.plusSeconds(1800))),
            Arguments.of("Duration too short", new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(), now.plusSeconds(3600), now.plusSeconds(4200))), // 20 min
            Arguments.of("Duration too long", new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(), now.plusSeconds(3600), now.plusSeconds(90000))), // 25 hours
            Arguments.of("Start time in past", new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(), now.minusSeconds(3600), now.plusSeconds(3600)))
        );
    }
}