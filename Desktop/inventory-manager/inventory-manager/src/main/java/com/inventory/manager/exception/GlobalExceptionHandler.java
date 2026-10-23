package com.inventory.manager.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto>handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        ErrorResponseDto dto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto>handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage="Validation failed";
        if (e.getFieldError()!=null && e.getFieldError().getDefaultMessage()!=null) {
            errorMessage=e.getFieldError().getDefaultMessage();
        }
        ErrorResponseDto dto = new ErrorResponseDto(
             LocalDateTime.now(),
             HttpStatus.BAD_REQUEST.value(),
             HttpStatus.BAD_REQUEST.getReasonPhrase(),
             errorMessage,
             request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
    }

}
