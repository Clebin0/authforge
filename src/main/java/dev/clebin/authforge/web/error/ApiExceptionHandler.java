package dev.clebin.authforge.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value."),
                (first, second) -> first,
                LinkedHashMap::new
            ));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
            "One or more request fields are invalid.", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
            "The request body is missing or malformed.", request, Map.of());
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
            "An account already exists for this email address.", request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleCredentials(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
            "Email or password is incorrect.", request, Map.of());
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ResponseEntity<ApiError> handleRefreshReuse(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE",
            "Refresh token reuse was detected. Sign in again.", request, Map.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleRefreshToken(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
            "The refresh token is invalid, expired, or revoked.", request, Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
            "The requested user does not exist.", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT",
            "The request conflicts with existing data.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "An unexpected server error occurred.", request, Map.of());
    }

    private ResponseEntity<ApiError> response(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request,
        Map<String, String> fields
    ) {
        return ResponseEntity.status(status).body(new ApiError(
            Instant.now(), status.value(), code, message, request.getRequestURI(), fields
        ));
    }
}
