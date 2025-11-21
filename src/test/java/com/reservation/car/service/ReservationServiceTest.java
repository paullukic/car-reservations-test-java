package com.reservation.car.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.entity.Car;
import com.reservation.car.entity.Reservation;
import com.reservation.car.exception.CarNotFoundException;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.exception.InvalidReservationException;
import com.reservation.car.model.ReservationStatus;
import com.reservation.car.repository.CarRepository;
import com.reservation.car.repository.ReservationRepository;
import com.reservation.car.util.TestConstants;
import com.reservation.car.util.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Car car;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        car = TestDataFactory.createTestCar(TestDataFactory.getTestCarId(), "Tesla", "Model 3", "ABC-123");
        reservation = TestDataFactory.createTestReservation(TestDataFactory.getTestReservationId(), TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.TWO_HOURS_SECONDS));
    }

    @Test
    void shouldCreateReservationSuccessfully_whenValidRequest() {
        // Arrange
        ReservationRequestDTO request = new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS));
        when(carRepository.findById(TestDataFactory.getTestCarId())).thenReturn(Optional.of(car));
        when(reservationRepository.hasOverlappingConfirmedReservation(any(), any(), any())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any())).thenReturn(reservation);

        // Act
        ReservationResponseDTO response = reservationService.createReservation(request, TestDataFactory.getTestUserId());

        // Assert
        assertThat(response.getId()).isEqualTo(TestDataFactory.getTestReservationId());
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void shouldThrowCarNotFoundException_whenCarDoesNotExist() {
        // Arrange
        ReservationRequestDTO request = new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS));
        when(carRepository.findById(TestDataFactory.getTestCarId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(request, TestDataFactory.getTestUserId()))
            .isInstanceOf(CarNotFoundException.class);
    }

    @Test
    void shouldThrowCarUnavailableException_whenDatabaseConflict() {
        // Arrange
        ReservationRequestDTO request = new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS));
        when(carRepository.findById(TestDataFactory.getTestCarId())).thenReturn(Optional.of(car));
        when(reservationRepository.hasOverlappingConfirmedReservation(any(), any(), any())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("conflicting key value violates exclusion constraint"));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(request, TestDataFactory.getTestUserId()))
            .isInstanceOf(CarUnavailableException.class);
    }

    @Test
    void shouldCancelReservationSuccessfully_whenValid() {
        // Arrange
        reservation.setStartTime(Instant.now().plusSeconds(TestConstants.TWO_HOURS_SECONDS));
        when(reservationRepository.findById(TestDataFactory.getTestReservationId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenReturn(reservation);

        // Act
        ReservationResponseDTO response = reservationService.cancelReservation(TestDataFactory.getTestReservationId(), TestDataFactory.getTestUserId());

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void shouldThrowInvalidReservationException_whenCancellationTooLate() {
        // Arrange
        Reservation lateReservation = TestDataFactory.createTestReservation(
            TestDataFactory.getTestReservationId(),
            TestDataFactory.getTestCarId(),
            TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.THIRTY_MINUTES_SECONDS), // too late to cancel
            Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS)); // end time (length irrelevant for cancellation)
        when(reservationRepository.findById(TestDataFactory.getTestReservationId())).thenReturn(Optional.of(lateReservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancelReservation(TestDataFactory.getTestReservationId(), TestDataFactory.getTestUserId()))
            .isInstanceOf(InvalidReservationException.class)
            .hasMessageContaining("Cancellation must be at least");
    }

    @Test
    void shouldThrowInvalidReservationException_whenNotOwner() {
        // Arrange
        UUID wrongUserId = UUID.randomUUID();
        when(reservationRepository.findById(TestDataFactory.getTestReservationId())).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancelReservation(TestDataFactory.getTestReservationId(), wrongUserId))
            .isInstanceOf(InvalidReservationException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    void shouldReturnAlreadyCancelledReservation_whenCancellingAlreadyCancelled() {
        // Arrange
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(TestDataFactory.getTestReservationId())).thenReturn(Optional.of(reservation));

        // Act
        ReservationResponseDTO response = reservationService.cancelReservation(TestDataFactory.getTestReservationId(), TestDataFactory.getTestUserId());

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.getId()).isEqualTo(TestDataFactory.getTestReservationId());
    }

    @Test
    void shouldRetryOnConcurrencyFailureAndSucceed() {
        // Arrange
        ReservationRequestDTO request = new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS));
        when(carRepository.findById(TestDataFactory.getTestCarId())).thenReturn(Optional.of(car));
        when(reservationRepository.hasOverlappingConfirmedReservation(any(), any(), any())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any()))
            .thenThrow(new ConcurrencyFailureException("deadlock detected"))
            .thenReturn(reservation);

        // Act
        ReservationResponseDTO response = reservationService.createReservation(request, TestDataFactory.getTestUserId());

        // Assert
        assertThat(response.getId()).isEqualTo(TestDataFactory.getTestReservationId());
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository, times(2)).saveAndFlush(any());
    }

    @Test
    void shouldThrowCarUnavailableException_afterMaxRetries() {
        // Arrange
        ReservationRequestDTO request = new ReservationRequestDTO(TestDataFactory.getTestCarId(), TestDataFactory.getTestUserId(),
            Instant.now().plusSeconds(TestConstants.ONE_HOUR_SECONDS), Instant.now().plusSeconds(TestConstants.THREE_HOURS_SECONDS));
        when(carRepository.findById(TestDataFactory.getTestCarId())).thenReturn(Optional.of(car));
        when(reservationRepository.hasOverlappingConfirmedReservation(any(), any(), any())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any()))
            .thenThrow(new ConcurrencyFailureException("deadlock 1"))
            .thenThrow(new ConcurrencyFailureException("deadlock 2"))
            .thenThrow(new ConcurrencyFailureException("deadlock 3"));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(request, TestDataFactory.getTestUserId()))
            .isInstanceOf(CarUnavailableException.class)
            .hasMessageContaining("persistent concurrency conflicts");
        verify(reservationRepository, times(3)).saveAndFlush(any());
    }
}