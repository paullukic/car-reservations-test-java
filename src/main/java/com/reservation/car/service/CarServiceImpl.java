package com.reservation.car.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reservation.car.dto.response.CarResponseDTO;
import com.reservation.car.entity.Car;
import com.reservation.car.exception.InvalidReservationException;
import com.reservation.car.repository.CarRepository;
import com.reservation.car.util.TimeSlotValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;

    /**
     * Retrieves all cars with pagination.
     * 
     * @param page page number (0-based)
     * @param size number of cars per page (recommended: 50-200 for UI)
     * @return paginated response with car DTOs and metadata
     */
    public Page<CarResponseDTO> getAllCars(int page, int size) {
        log.info("Retrieving cars page {} with size {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("make", "model"));
        Page<Car> carsPage = carRepository.findAllCarsPaginated(pageable);
        
        return carsPage.map(CarResponseDTO::from);
    }

    /**
     * Finds available cars for the time period with pagination.
     * 
     * @param startTime the desired start time
     * @param endTime the desired end time
     * @param page page number (0-based)
     * @param size number of cars per page
     * @return paginated response with available car DTOs
     * @throws InvalidReservationException if time period is invalid
     */
    public Page<CarResponseDTO> findAvailableCars(Instant startTime, Instant endTime, int page, int size) {
        log.info("Finding available cars from {} to {}, page {} size {}", startTime, endTime, page, size);
        
        TimeSlotValidator.validateTimeSlot(startTime, endTime);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("make", "model"));
        Page<Car> carsPage = carRepository.findAvailableCarsForTimePeriod(startTime, endTime, pageable);
        
        return carsPage.map(CarResponseDTO::from);
    }


}
