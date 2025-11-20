package com.reservation.car.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.reservation.car.entity.Car;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {
    
    /**
     * Finds cars available for booking during the specified time period with pagination.
     */
    @Query(value = "SELECT c.* FROM car c " +
           "WHERE NOT EXISTS (" +
           "  SELECT 1 FROM reservation r " +
           "  WHERE r.car_id = c.id " +
           "  AND r.status = 'CONFIRMED' " +
           "  AND r.start_time < :endTime " +
           "  AND r.end_time > :startTime" +
           ") " +
           "ORDER BY c.make, c.model, c.license_plate", 
           nativeQuery = true)
    Page<Car> findAvailableCarsForTimePeriod(
        @Param("startTime") Instant startTime, 
        @Param("endTime") Instant endTime,
        Pageable pageable
    );
    
    /**
     * Finds all cars with pagination support for large fleets.
     * Optimized with proper ordering for consistent results across pages.
     */
    @Query("SELECT c FROM Car c ORDER BY c.make, c.model, c.licensePlate")
    Page<Car> findAllCarsPaginated(Pageable pageable);
}
