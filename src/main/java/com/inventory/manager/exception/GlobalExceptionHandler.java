package com.inventory.manager.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(dto(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Validation failed" : message, path(request)));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(dto(HttpStatus.BAD_REQUEST, "Malformed JSON request body", path(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String message = body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null
                ? problemDetail.getDetail()
                : "Request could not be processed";
        return ResponseEntity.status(statusCode).headers(headers)
                .body(dto(statusCode, message, path(request)));
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResource(DuplicateResourceException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(BadRequestException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access denied: You do not have permission to access this resource", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource", request);
    }


    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleOptimisticLock(OptimisticLockingFailureException e, HttpServletRequest request) {
        log.warn("Optimistic lock failure on {} {}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.CONFLICT, "The resource was modified by another request. Please reload and try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        Throwable cause = e.getMostSpecificCause();
        log.warn("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(), cause.getMessage());
        String sqlState = cause instanceof SQLException sqlException ? sqlException.getSQLState() : null;

        if ("23505".equals(sqlState)) {
            return error(HttpStatus.CONFLICT, "A record with the same unique value already exists", request);
        }
        if ("23503".equals(sqlState)) {
            return error(HttpStatus.CONFLICT, "This operation conflicts with related records (the resource references or is referenced by other records)", request);
        }
        if ("23514".equals(sqlState) || "23502".equals(sqlState) || "22001".equals(sqlState)) {
            return error(HttpStatus.BAD_REQUEST, "The submitted data violates a database rule", request);
        }
        return error(HttpStatus.CONFLICT, "Database constraint violation", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponseDto> handlePropertyReference(PropertyReferenceException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid sort or filter property: " + e.getPropertyName(), request);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidDataAccess(InvalidDataAccessApiUsageException e, HttpServletRequest request) {
        if (e.getMostSpecificCause() instanceof PropertyReferenceException propertyException) {
            return error(HttpStatus.BAD_REQUEST, "Invalid sort or filter property: " + propertyException.getPropertyName(), request);
        }
        log.error("Invalid data access usage on {} {}", request.getMethod(), request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }


    private ErrorResponseDto dto(HttpStatusCode status, String message, String path) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : status.toString();
        return new ErrorResponseDto(LocalDateTime.now(), status.value(), reason, message, path);
    }

    private ResponseEntity<ErrorResponseDto> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(dto(status, message, request.getRequestURI()));
    }

    private String path(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getRequestURI()
                : "";
    }
}