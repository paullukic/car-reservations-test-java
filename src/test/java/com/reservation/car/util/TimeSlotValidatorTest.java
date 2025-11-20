package com.reservation.car.util;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.reservation.car.exception.InvalidReservationException;

class TimeSlotValidatorTest {

    @Test
    void shouldThrowException_whenEndTimeBeforeStartTime() {
        // Arrange
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.minusSeconds(1800);

        // Act & Assert
        assertThatThrownBy(() -> TimeSlotValidator.validateTimeSlot(start, end))
            .isInstanceOf(InvalidReservationException.class)
            .hasMessage("Invalid reservation: End time must be after start time");
    }

    @Test
    void shouldThrowException_whenDurationTooShort() {
        // Arrange
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600); // 1 hour

        // Act & Assert
        assertThatThrownBy(() -> TimeSlotValidator.validateTimeSlot(start, end))
            .isInstanceOf(InvalidReservationException.class)
            .hasMessage("Invalid reservation: Reservation must be at least 2 hours");
    }

    @Test
    void shouldThrowException_whenDurationTooLong() {
        // Arrange
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(25 * 3600); // 25 hours

        // Act & Assert
        assertThatThrownBy(() -> TimeSlotValidator.validateTimeSlot(start, end))
            .isInstanceOf(InvalidReservationException.class)
            .hasMessage("Invalid reservation: Reservation cannot exceed 24 hours");
    }

    @Test
    void shouldPass_whenValidTimeSlot() {
        // Arrange
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3 * 3600); // 3 hours

        // Act
        TimeSlotValidator.validateTimeSlot(start, end);

        // Assert
        // No exception thrown, test passes
    }
}