package org.alexstpm.fms.common.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> responseStatus(ResponseStatusException ex) {
        String error = switch (ex.getStatusCode().value()) {
            case 400 -> "BAD_REQUEST";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> "REQUEST_FAILED";
        };

        String message = ex.getReason() == null ? "Request failed" : ex.getReason();

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiError(error, message));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("AUTHENTICATION_FAILED", "Invalid email or password"));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<ApiError> authorization(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("ACCESS_DENIED", "Access denied"));
    }

    @ExceptionHandler(JwtException.class)
    ResponseEntity<ApiError> jwt(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_TOKEN", "Invalid or expired token"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_FAILED",
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining("; "))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("MALFORMED_JSON", "Malformed JSON request"));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ApiError> invalidSort(PropertyReferenceException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_SORT",
                        "Unknown sort property: " + ex.getPropertyName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_PARAMETER",
                        "Invalid value for parameter '" + ex.getName() + "'"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation ->
                        violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "DATA_CONFLICT",
                        "The request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();

            String error = switch (status.value()) {
                case 400 -> "BAD_REQUEST";
                case 401 -> "UNAUTHORIZED";
                case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 406 -> "NOT_ACCEPTABLE";
                case 409 -> "CONFLICT";
                case 415 -> "UNSUPPORTED_MEDIA_TYPE";
                case 500 -> "INTERNAL_SERVER_ERROR";
                default -> "REQUEST_FAILED";
            };

            String message = switch (status.value()) {
                case 400 -> "Request is invalid";
                case 401 -> "Authentication required";
                case 403 -> "Access denied";
                case 404 -> "Resource not found";
                case 405 -> "HTTP method is not allowed for this endpoint";
                case 406 -> "Requested response format is not supported";
                case 409 -> "The request conflicts with existing data";
                case 415 -> "Request media type is not supported";
                default -> "Request could not be processed";
            };

            return ResponseEntity.status(status)
                    .headers(springError.getHeaders())
                    .body(new ApiError(error, message));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected server error"));
    }
}
