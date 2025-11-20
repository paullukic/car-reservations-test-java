package com.reservation.car.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.reservation.car.config.ErrorCode;
import com.reservation.car.dto.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the entire API.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CarUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCarUnavailable(CarUnavailableException ex, WebRequest request) {
        log.warn("{}: {}", ErrorCode.CAR_UNAVAILABLE.getCode(), ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.CAR_UNAVAILABLE.getCode(),
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CarNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCarNotFound(CarNotFoundException ex, WebRequest request) {
        log.warn("{}: {}", ErrorCode.CAR_NOT_FOUND.getCode(), ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.CAR_NOT_FOUND.getCode(),
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidReservationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReservation(InvalidReservationException ex, WebRequest request) {
        log.warn("{}: {}", ErrorCode.INVALID_RESERVATION.getCode(), ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.INVALID_RESERVATION.getCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", ""),
            details
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", ""),
            details
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.INVALID_ARGUMENT.getCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.MISSING_PARAMETER.getCode(),
            ErrorCode.MISSING_PARAMETER.getFormattedMessage(ex.getParameterName()),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, WebRequest request) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.MISSING_HEADER.getCode(),
            ErrorCode.MISSING_HEADER.getFormattedMessage(ex.getHeaderName()),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch for parameter {}: expected {}, got {}", 
            ex.getName(), ex.getRequiredType(), ex.getValue());
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.INVALID_PARAMETER_TYPE.getCode(),
            ErrorCode.INVALID_PARAMETER_TYPE.getFormattedMessage(ex.getName()),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, WebRequest request) {
        log.error("Database access error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.DATABASE_ERROR.getCode(),
            ErrorCode.DATABASE_ERROR.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
            ErrorCode.INTERNAL_ERROR.getCode(),
            ErrorCode.INTERNAL_ERROR.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}