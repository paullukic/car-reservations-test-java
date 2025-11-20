package com.reservation.car.util;

import java.time.Instant;
import java.util.UUID;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.CarResponseDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.entity.Car;
import com.reservation.car.entity.Reservation;
import com.reservation.car.model.ReservationStatus;

/**
 * Utility class for creating test data objects.
 */
public class TestDataFactory {

    // Fixed test IDs for consistent testing across tests
    public static UUID getTestCarId() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }

    public static UUID getTestCarId2() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    public static UUID getTestUserId() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    }

    public static UUID getTestReservationId() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    }

    public static Car createTestCar(UUID id, String make, String model, String licensePlate) {
        Car car = new Car(make, model, licensePlate);
        car.setId(id);
        return car;
    }

    public static Car createTestCar(String make, String model, String licensePlate) {
        Car car = new Car(make, model, licensePlate);
        car.setId(UUID.randomUUID());
        return car;
    }

    public static Car createTestCar() {
        return createTestCar(getTestCarId(), "Tesla", "Model 3", "ABC-123");
    }

    public static Reservation createTestReservation(UUID reservationId, UUID carId, UUID userId, Instant startTime, Instant endTime) {
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setCarId(carId);
        reservation.setUserId(userId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.now());
        return reservation;
    }

    public static Reservation createTestReservation(UUID carId, UUID userId, Instant startTime, Instant endTime) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setCarId(carId);
        reservation.setUserId(userId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.now());
        return reservation;
    }

    public static ReservationRequestDTO createReservationRequestDTO(UUID carId, UUID userId, Instant startTime, Instant endTime) {
        return new ReservationRequestDTO(carId, userId, startTime, endTime);
    }

    public static ReservationResponseDTO createReservationResponseDTO(UUID reservationId, UUID carId, UUID userId,
                                                                       Instant startTime, Instant endTime,
                                                                       ReservationStatus status, Instant createdAt) {
        return new ReservationResponseDTO(reservationId, carId, userId, startTime, endTime, status, createdAt);
    }

    public static CarResponseDTO createCarResponseDTO(UUID id, String make, String model, String licensePlate) {
        return new CarResponseDTO(id, make, model, licensePlate);
    }
}