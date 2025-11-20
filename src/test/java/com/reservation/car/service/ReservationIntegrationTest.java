package com.reservation.car.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.entity.Reservation;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.model.ReservationStatus;
import com.reservation.car.util.TestDataFactory;

@SpringBootTest
@Testcontainers
class ReservationIntegrationTest extends BaseReservationIntegrationTest {

    /**
     * Tests concurrency: only one overlapping reservation succeeds via exclusion constraint.
     */
    @Test
    void shouldAllowOnlyOneReservationWhenConcurrentRequestsOverlap() throws InterruptedException {
        // ARRANGE: Define the time slot and two identical requests
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS); // 3-hour booking is valid

        ReservationRequestDTO request = new ReservationRequestDTO(
            testCar.getId(), TestDataFactory.getTestUserId(), start, end
        );

        // ACT: Use an ExecutorService to simulate two simultaneous calls
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Task 1: Attempt to book
        Future<Boolean> result1 = executor.submit(() -> {
            try {
                reservationService.createReservation(request, TestDataFactory.getTestUserId());
                return true; // Success
            } catch (CarUnavailableException e) {
                return false; // Expected failure (due to conflict)
            }
        });

        // Task 2: Attempt to book (identical, concurrent request)
        Future<Boolean> result2 = executor.submit(() -> {
            try {
                reservationService.createReservation(request, TestDataFactory.getTestUserId());
                return true; // Success
            } catch (CarUnavailableException e) {
                return false; // Expected failure (due to conflict)
            }
        });

        // SHUTDOWN and WAIT
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            // Forcefully interrupt tasks if they are stuck
            executor.shutdownNow();
        }

        // ASSERT: Check the results and the final database state
        // 1. Exactly one Future should be true (success) and one false (failure)
        long successCount = Stream.of(result1, result2)
            .filter(Future::isDone)
            .map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    return false;
                }
            })
            .filter(Boolean::booleanValue)
            .count();

        assertThat(successCount).as("Only one concurrent request should succeed").isEqualTo(1);
        
        // 2. The database should only contain one CONFIRMED reservation
        assertThat(reservationRepository.count()).as("Only one row should exist in the database").isEqualTo(1);
        
        Reservation finalReservation = reservationRepository.findAll().get(0);
        assertThat(finalReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
}