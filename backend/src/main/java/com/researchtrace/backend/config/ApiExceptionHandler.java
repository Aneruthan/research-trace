package com.researchtrace.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Returns the reason of deliberate API errors in the {@code message} field. Other exceptions keep
 * Spring Boot's default error body, which omits exception messages.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {
        return errorBody(exception.getStatusCode(), exception.getHeaders(), exception.getReason(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return errorBody(HttpStatus.CONFLICT, HttpHeaders.EMPTY,
            "This change conflicts with existing data", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequestBody(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining("; "));
        return errorBody(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY,
            message.isEmpty() ? "Invalid request body" : message, request);
    }

    private static ResponseEntity<Map<String, Object>> errorBody(
            HttpStatusCode status,
            HttpHeaders headers,
            String message,
            HttpServletRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", resolved != null ? resolved.getReasonPhrase() : "");
        body.put("message", message != null ? message : "");
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).headers(headers).body(body);
    }
}
