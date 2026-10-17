package com.microinvestment.simulator.exception;

import com.microinvestment.simulator.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// INTERVIEW: controller advice centralizes exception translation and keeps controllers focused on HTTP actions.
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                        HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(ResourceConflictException exception,
                                                               HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(SimulatorException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(SimulatorException exception,
                                                            HttpServletRequest request) {
        // INTERVIEW: unchecked domain exceptions automatically trigger rollback of Spring transactions.
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConcurrentModification(Exception exception,
                                                                      HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "The account changed during this request. Please retry.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception,
                                                            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "The request conflicts with an existing record or account limit.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException exception,
                                                        HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "You are not allowed to access this resource.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception,
                                                             HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                         HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request validation failed.";
        }
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                             HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "The request body contains invalid JSON or an unsupported value.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
                                                                  HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getName() + " has an invalid value.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception,
                                                               HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getParameterName() + " is required.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException exception,
                                                              HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "The requested resource was not found.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethod(HttpRequestMethodNotSupportedException exception,
                                                                HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for this resource.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
                                                                   HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use the application/json content type.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        // INTERVIEW: expose a safe message and log only the exception type, never credentials or raw request bodies.
        LOGGER.error("Unhandled request exception: {}", exception.getClass().getName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
