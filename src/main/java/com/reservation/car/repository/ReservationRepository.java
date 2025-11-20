package com.reservation.car.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.reservation.car.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    
    /**
     * Checks if there are any confirmed reservations for a specific car
     * that overlap with the given time period.
     * 
     * @param carId the car to check
     * @param startTime the start of the time period to check
     * @param endTime the end of the time period to check
     * @return true if there are overlapping reservations, false otherwise
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r "
         + "WHERE r.carId = :carId "
         + "AND r.status = 'CONFIRMED' "
         + "AND r.startTime < :endTime "
         + "AND r.endTime > :startTime")
    boolean hasOverlappingConfirmedReservation(
        @Param("carId") UUID carId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
