package com.reservation.car.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.util.TestDataFactory;

/**
 * Chaos / stress style integration tests.
 *
 * IMPORTANT: Keep determinism. Do NOT use randomness or rely on timing races.
 * These tests must either always pass or always fail – never “sometimes”.
 */
@SpringBootTest
@Testcontainers
class ReservationIntegrationChaosTest extends BaseReservationIntegrationTest {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(5);
    private static final int CONCURRENT_THREAD_COUNT = 5;

    /**
     * Stress test: 5 concurrent overlapping reservation attempts for the SAME car & time slot.
     * Only one should succeed; the rest must fail with CarUnavailableException due to exclusion constraint.
     *
     * Deterministic concurrency: tasks start together using CountDownLatch.
     */
    @Test
    void shouldAllowOnlyOneSuccess_amongFiveConcurrentOverlappingReservations() throws InterruptedException {
        // ARRANGE
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS);
        ReservationRequestDTO request =
            new ReservationRequestDTO(testCar.getId(), TestDataFactory.getTestUserId(), start, end);

        int threads = CONCURRENT_THREAD_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                startGun.await();
                try {
                    reservationService.createReservation(request, TestDataFactory.getTestUserId());
                    return true;
                } catch (CarUnavailableException e) {
                    return false;
                }
            }));
        }

        // Ensure all threads are queued
        ready.await(3, TimeUnit.SECONDS);
        // ACT
        startGun.countDown();

        // ASSERT
        int successes = 0;
        for (Future<Boolean> f : futures) {
            try {
                if (Boolean.TRUE.equals(f.get(OPERATION_TIMEOUT.getSeconds(), TimeUnit.SECONDS))) {
                    successes++;
                }
            } catch (ExecutionException | TimeoutException e) {
                // Treat any unexpected failure as non-success.
            }
        }
        executor.shutdownNow();

        // Exactly one success is the invariant.
        Assertions.assertThat(successes).isEqualTo(1);
        Assertions.assertThat(reservationRepository.count()).isEqualTo(1);
    }
}