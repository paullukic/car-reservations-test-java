package com.reservation.car.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reservation.car.dto.ReservationRequestDTO;
import com.reservation.car.dto.response.ReservationResponseDTO;
import com.reservation.car.entity.Reservation;
import com.reservation.car.exception.CarNotFoundException;
import com.reservation.car.exception.CarUnavailableException;
import com.reservation.car.exception.InvalidReservationException;
import com.reservation.car.model.ReservationStatus;
import com.reservation.car.repository.CarRepository;
import com.reservation.car.repository.ReservationRepository;
import com.reservation.car.util.DatabaseUtils;
import com.reservation.car.util.TimeSlotValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final Duration MIN_CANCELLATION_NOTICE = Duration.ofMinutes(30);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ReservationRepository reservationRepository;
    private final CarRepository carRepository;

    @Override
    public ReservationResponseDTO createReservation(ReservationRequestDTO request, UUID requestingUserId) {
        validateAuthorization(request, requestingUserId);
        
        log.info("Creating reservation for car {} from {} to {}", 
                request.getCarId(), request.getStartTime(), request.getEndTime());
        
        validateReservationRequest(request);
        
        Reservation reservation = createReservationEntity(request);
        
        Reservation saved = saveReservationWithRetry(reservation);
        
        log.info("Successfully created reservation with ID {}", saved.getId());
        return ReservationResponseDTO.from(saved);
    }

    @Override
    public ReservationResponseDTO cancelReservation(UUID reservationId, UUID userId) {
        log.info("Cancelling reservation {} for user {}", reservationId, userId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new InvalidReservationException(
                "Reservation with ID " + reservationId + " not found"));
        
        if (!reservation.getUserId().equals(userId)) {
            throw new InvalidReservationException(
                "User " + userId + " is not authorized to cancel this reservation");
        }
        
        // If already cancelled, return as is
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationResponseDTO.from(reservation);
        }
        
        // Cancellation requires at least 30 minutes before start time
        Instant now = Instant.now();
        Duration timeUntilStart = Duration.between(now, reservation.getStartTime());
        
        if (timeUntilStart.compareTo(MIN_CANCELLATION_NOTICE) < 0) {
            throw new InvalidReservationException(
                String.format("Cancellation must be at least %d minutes before start time. "
                    + "Current time until start: %d minutes", 
                    MIN_CANCELLATION_NOTICE.toMinutes(), 
                    timeUntilStart.toMinutes()));
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation cancelled = reservationRepository.save(reservation);
        
        log.info("Successfully cancelled reservation {}", reservationId);
        return ReservationResponseDTO.from(cancelled);
    }

    private void validateAuthorization(ReservationRequestDTO request, UUID requestingUserId) {
        if (!request.getUserId().equals(requestingUserId)) {
            throw new InvalidReservationException(
                "User " + requestingUserId + " is not authorized to create reservation for user " + request.getUserId());
        }
    }

    private void validateReservationRequest(ReservationRequestDTO request) {
        TimeSlotValidator.validateTimeSlot(request.getStartTime(), request.getEndTime());
        
        carRepository.findById(request.getCarId())
            .orElseThrow(() -> new CarNotFoundException(
                "Car with ID " + request.getCarId() + " not found"));
        
        // Check for overlapping reservations
        // This provides fast-fail for most conflicts and better user experience
        // Note: The check is repeated in saveReservationWithRetry() to handle race conditions
        boolean hasOverlap = reservationRepository.hasOverlappingConfirmedReservation(
            request.getCarId(), 
            request.getStartTime(), 
            request.getEndTime()
        );
        
        if (hasOverlap) {
            throw new CarUnavailableException(
                "Car is not available for the requested time slot. Another reservation already exists.");
        }
    }

    private Reservation createReservationEntity(ReservationRequestDTO request) {
        Reservation reservation = new Reservation();
        reservation.setCarId(request.getCarId());
        reservation.setUserId(request.getUserId());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.now());
        return reservation;
    }

    private Reservation saveReservationWithRetry(Reservation reservation) {
        int attempts = 0;
        // Retry loop for handling concurrency issues
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                // APPLICATION CHECK: Verify no conflicts before insert (fast B-tree index scan)
                boolean hasOverlap = reservationRepository.hasOverlappingConfirmedReservation(
                    reservation.getCarId(), 
                    reservation.getStartTime(), 
                    reservation.getEndTime()
                );
                
                if (hasOverlap) {
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        throw new CarUnavailableException(
                            "Car is not available for the requested time slot. Multiple overlapping requests detected.");
                    }
                    log.debug("Overlap detected on attempt {}, retrying after backoff", attempts);
                    try {
                        Thread.sleep(attempts * 100); // 100ms, 200ms, 300ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Reservation creation interrupted", ie);
                    }
                    continue; // Retry the check
                }
                
                // No conflict detected, proceed with insert
                return reservationRepository.saveAndFlush(reservation);
                
            } catch (ConcurrencyFailureException e) {
                attempts++;
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("Failed to create reservation after {} attempts due to concurrency issue: {}", MAX_RETRY_ATTEMPTS, e.getMessage(), e);
                    throw new CarUnavailableException("Failed to create reservation due to persistent concurrency conflicts (e.g., deadlocks or serialization failures). Please try again later.", e);
                }
                log.debug("Concurrency issue on attempt {} for reservation creation, retrying: {}", attempts, e.getMessage(), e);
                try {
                    Thread.sleep(100 * attempts); // Exponential backoff: 100ms, 200ms, 300ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Reservation creation interrupted", ie);
                }
            } catch (DataIntegrityViolationException e) {
                // DB constraint is now a SAFETY NET, not primary validation
                // This should rarely happen now that we check in the application
                if (DatabaseUtils.isExclusionConstraintViolation(e)) {
                    log.warn("Database constraint caught overlap that application missed", e);
                    throw new CarUnavailableException(
                        "Car is not available for the requested time slot. Another reservation overlaps.", e);
                } else {
                    throw new InvalidReservationException(
                        "Reservation request violates database constraints: " + e.getMessage(), e);
                }
            } catch (DataAccessException e) {
                // Handle specific database access issues
                if (e instanceof QueryTimeoutException) {
                    log.error("Database query timeout during reservation creation", e);
                    throw new RuntimeException("Database query timed out. Please try again later.", e);
                } else if (e instanceof DataAccessResourceFailureException) {
                    log.error("Database connection failure during reservation creation", e);
                    throw new RuntimeException("Database connection temporarily unavailable. Please try again later.", e);
                } else {
                    log.error("General database access error during reservation creation", e);
                    throw new RuntimeException("Database temporarily unavailable. Please try again later.", e);
                }
            }
        }
        // This should never be reached, but added for completeness
        throw new RuntimeException("Unexpected error in reservation creation");
    }

}
