package io.github.kitamura.subtrack_api.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // =====================================================
    // Handle CustomException (application-specific errors, status carried by the exception)
    // =====================================================
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorResponse err = new ErrorResponse(ex.getStatus().name(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(err);
    }

    // =====================================================
    // Handle IllegalArgumentException (bad request errors)
    // =====================================================
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse err = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.badRequest().body(err);
    }

    // =====================================================
    // Handle MethodArgumentTypeMismatchException (e.g. non-numeric path variable or X-User-Id header)
    // =====================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse err = new ErrorResponse("BAD_REQUEST", "Invalid value for parameter: " + ex.getName());
        return ResponseEntity.badRequest().body(err);
    }

    // =====================================================
    // Handle ConstraintViolationException (validation errors)
    // =====================================================
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse err = new ErrorResponse("VALIDATION_ERROR", msg);
        return ResponseEntity.badRequest().body(err);
    }

    // =====================================================
    // Handle MethodArgumentNotValidException (request body validation errors)
    // =====================================================
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse err = new ErrorResponse("VALIDATION_ERROR", msg);
        return ResponseEntity.status(status).headers(headers).body(err);
    }

    // =====================================================
    // Handle all other exceptions (internal errors)
    // Do not leak internal exception details to the client; log server-side instead.
    // =====================================================
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse err = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}