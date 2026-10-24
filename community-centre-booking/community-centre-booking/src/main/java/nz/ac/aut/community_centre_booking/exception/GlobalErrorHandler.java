package nz.ac.aut.community_centre_booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import nz.ac.aut.community_centre_booking.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

// converts app exceptions into consistent HTTP responses
@RestControllerAdvice
public class GlobalErrorHandler {

    // returns the first validation error message for invalid requests
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("The request is invalid.");

        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    // A3: returns 400 for unreadable JSON, e.g. an unknown status value
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "The request body is missing or has an invalid value.", request);
    }

    // A3: returns 400 for invalid URL parameters, e.g. ?status=WRONG
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Invalid value for parameter '" + exception.getName() + "'.", request);
    }

    // returns a 404 response for resources that cannot be found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NoSuchElementException exception,
            HttpServletRequest request) {

        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    // returns a 400 response for invalid requests, such as invalid room IDs
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    // returns a 409 response for booking conflicts, such as overlapping bookings
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(
            IllegalStateException exception,
            HttpServletRequest request) {

        return build(HttpStatus.CONFLICT, "BOOKING_CONFLICT", exception.getMessage(), request);
    }

    // A3: returns 403 when a user is logged in but not allowed to do the action
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action.", request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String code, String message, HttpServletRequest request) {

        return ResponseEntity
                .status(status)
                .body(new ApiError(code, message, request.getRequestURI()));
    }
}
